package com.example.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.Anime
import com.example.data.model.AppNotification
import com.example.data.model.DownloadItem
import com.example.data.model.Episode
import com.example.data.resolver.HlsParser
import com.example.data.resolver.SubtitleTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Downloads the *real* resolved stream for an episode.
 *
 * HLS streams are saved as `.ts` (a container VLC, MX Player and the built-in player all accept —
 * concatenating MPEG-TS segments into a file named `.mp4` produces an unplayable file, which is what
 * the previous implementation did). Progressive streams keep their own extension.
 *
 * There is deliberately no fallback URL: when no stream can be resolved the download is marked
 * FAILED with the resolver's explanation instead of silently saving a placeholder video.
 */
class AnimeDownloadManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsManager: SettingsManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val TAG = "AnimeDownloader"
        private const val CHANNEL_ID = "zenith_downloads_channel"
        private const val CHANNEL_NAME = "Zenith Anime Downloads"
        private const val CHANNEL_SOUND_ID = "zenith_downloads_sound_channel"
        private const val CHANNEL_SOUND_NAME = "Zenith Anime Download Alerts"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_QUEUED = "QUEUED"

        /** Subtitle tracks discovered by the player, keyed by download id. */
        val SUBTITLE_CACHE: MutableMap<String, SubtitleTrack> = ConcurrentHashMap()

        fun rememberSubtitle(animeId: String, episodeNumber: Int, track: SubtitleTrack) {
            SUBTITLE_CACHE["${animeId}_$episodeNumber"] = track
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val silentChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows live download progress" }
            val soundChannel = NotificationChannel(
                CHANNEL_SOUND_ID,
                CHANNEL_SOUND_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alerts when an episode download completes with sound enabled" }
            manager?.createNotificationChannel(silentChannel)
            manager?.createNotificationChannel(soundChannel)
        }
    }

    /**
     * Starts (or restarts) a download for [episode].
     * [streamUrl] must be a real resolved URL; callers resolve one first when it is missing.
     */
    fun enqueueDownload(
        anime: Anime,
        episode: Episode,
        quality: String = "1080p",
        streamUrl: String? = null,
        headers: Map<String, String> = emptyMap(),
        subtitles: List<SubtitleTrack> = emptyList()
    ) {
        val downloadId = "${anime.id}_${episode.number}"
        activeJobs[downloadId]?.cancel()

        val settings = settingsManager.getSettings()
        val safeTitle = anime.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(40)
        val isHls = streamUrl?.contains(".m3u8") == true
        val targetDir = targetDirectory(safeTitle)

        if (subtitles.isNotEmpty()) {
            rememberSubtitle(anime.id, episode.number, subtitles.first())
        }

        val placeholder = DownloadItem(
            id = downloadId,
            animeId = anime.id,
            animeTitle = anime.title,
            episodeNumber = episode.number,
            episodeTitle = episode.title,
            thumbnail = episode.thumbnail.ifEmpty { anime.bannerImage },
            quality = quality,
            fileSize = "0 MB",
            downloadedAt = System.currentTimeMillis(),
            filePath = "",
            status = if (streamUrl.isNullOrBlank()) STATUS_FAILED else STATUS_QUEUED,
            progress = 0,
            streamUrl = streamUrl.orEmpty()
        )

        val job = scope.launch {
            database.downloadDao().insertDownload(placeholder)
            updateProgress(downloadId, 0)

            if (streamUrl.isNullOrBlank() || !streamUrl.startsWith("http")) {
                fail(downloadId, "No stream URL available for this episode")
                return@launch
            }

            val concurrency = settings.downloadConcurrency.coerceIn(1, 4)
            if (WIFI_ONLY_VIOLATED) {
                fail(downloadId, "Wi-Fi only downloads are enabled")
                return@launch
            }
            semaphore(concurrency).withPermit {
                runDownload(downloadId, anime, episode, quality, streamUrl, headers, isHls, targetDir, safeTitle, settings.downloadNotifications)
            }
        }
        activeJobs[downloadId] = job
    }

    private val semaphores = ConcurrentHashMap<Int, Semaphore>()
    private fun semaphore(permits: Int): Semaphore =
        semaphores.getOrPut(permits) { Semaphore(permits) }

    private val WIFI_ONLY_VIOLATED: Boolean
        get() = settingsManager.getSettings().wifiOnlyDownloads && !isOnWifi()

    private fun isOnWifi(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun runDownload(
        downloadId: String,
        anime: Anime,
        episode: Episode,
        quality: String,
        streamUrl: String,
        headers: Map<String, String>,
        declaredHls: Boolean,
        targetDir: File,
        safeTitle: String,
        notify: Boolean
    ) {
        var outFile: File? = null
        try {
            database.downloadDao().updateProgress(downloadId, 0, STATUS_DOWNLOADING, "0 MB")
            if (notify) postProgressNotification(downloadId.hashCode(), anime.title, episode.number, 0)

            val maxRetries = settingsManager.getSettings().downloadRetryCount.coerceIn(0, 5)
            var attempt = 0
            var lastError: Throwable? = null

            while (attempt <= maxRetries) {
                currentCoroutineContext().ensureActive()
                try {
                    val isHls = declaredHls || fetchLooksLikeHls(streamUrl, headers)
                    outFile = if (isHls) {
                        downloadHls(streamUrl, targetDir, safeTitle, episode.number, headers, downloadId, anime, notify, preferredQuality = quality)
                    } else {
                        downloadProgressive(streamUrl, targetDir, safeTitle, episode.number, headers, downloadId, anime, notify)
                    }

                    if (outFile == null || !outFile.exists() || outFile.length() <= 0L) {
                        throw IllegalStateException("Server returned an empty file")
                    }
                    lastError = null
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    lastError = t
                    outFile?.takeIf { it.exists() }?.delete()
                    attempt++
                    if (attempt <= maxRetries) {
                        Log.w(TAG, "Retry $attempt/$maxRetries for $downloadId after error: ${t.message}")
                        delay(1000L * attempt)
                    }
                }
            }

            if (lastError != null || outFile == null) {
                throw (lastError ?: IllegalStateException("Server returned an empty file"))
            }

            val sizeLabel = formatFileSize(outFile.length())
            database.downloadDao().markCompleted(downloadId, outFile.absolutePath, sizeLabel)
            updateProgress(downloadId, 100)

            // Subtitles next to the video when enabled.
            val subtitleSetting = settingsManager.getSettings().downloadSubtitles
            if (subtitleSetting != "Off" && subtitleSetting != "None") {
                writeSubtitle(downloadId, headers, targetDir, safeTitle, episode.number)
            }

            scan(outFile)
            val currentSettings = settingsManager.getSettings()
            if (currentSettings.downloadExportAfterFinish || currentSettings.downloadLocation == "Downloads folder") {
                exportToPublicFolder(outFile, safeTitle)
            }
            if (currentSettings.downloadCustomFolderUri.isNotBlank()) {
                copyToSafFolder(outFile, currentSettings.downloadCustomFolderUri)
            }
            // Completion notifications are a separate switch from live progress ones: a long
            // download may want the ongoing progress bar without a ping when it lands.
            if (currentSettings.downloadCompleteNotifications) {
                postCompleteNotification(downloadId.hashCode(), anime.title, episode.number)
            } else {
                cancelNotification(downloadId.hashCode())
            }

            database.notificationDao().insert(
                AppNotification(
                    id = "notif_${System.currentTimeMillis()}",
                    title = "Download complete",
                    message = "${anime.title} • Episode ${episode.number} saved ($sizeLabel)",
                    timestamp = "Just now",
                    animeThumbnail = anime.coverImage,
                    tag = "DOWNLOAD"
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Episode ${episode.number} downloaded — ${outFile.name} ($sizeLabel)",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: CancellationException) {
            outFile?.takeIf { it.exists() }?.delete()
            cancelNotification(downloadId.hashCode())
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "download failed for $downloadId", t)
            outFile?.takeIf { it.exists() }?.delete()
            fail(downloadId, t.message ?: t.javaClass.simpleName)
        } finally {
            activeJobs.remove(downloadId)
        }
    }

    private fun fail(downloadId: String, reason: String) {
        Log.w(TAG, "download $downloadId failed: $reason")
        scope.launch {
            database.downloadDao().updateProgress(downloadId, 0, STATUS_FAILED, reason)
            updateProgress(downloadId, 0)
            cancelNotification(downloadId.hashCode())
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: $reason", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun request(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
            .header(
                "User-Agent",
                headers["User-Agent"]
                    ?: "Mozilla/5.0 (Linux; Android 15; SM-S166V) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            )
        headers.forEach { (key, value) ->
            if (!key.equals("User-Agent", true) && value.isNotBlank()) {
                try {
                    builder.header(key, value)
                } catch (_: Throwable) {
                }
            }
        }
        return builder.build()
    }

    /** Sniffs the first bytes of a URL that has no obvious extension to decide how to save it. */
    private fun fetchLooksLikeHls(url: String, headers: Map<String, String>): Boolean {
        if (url.contains(".m3u8")) return true
        return try {
            httpClient.newCall(request(url, headers)).execute().use { response ->
                val stream = response.body?.byteStream() ?: return false
                val prefix = ByteArray(64)
                val read = stream.read(prefix, 0, prefix.size)
                if (read <= 0) return false
                String(prefix, 0, read, Charsets.UTF_8).contains("#EXTM3U")
            }
        } catch (_: Throwable) {
            false
        }
    }

    /** Downloads an HLS stream into a single `.ts` file, selecting the requested quality. */
    private suspend fun downloadHls(
        m3u8Url: String,
        targetDir: File,
        safeTitle: String,
        episodeNumber: Int,
        headers: Map<String, String>,
        downloadId: String,
        anime: Anime,
        notify: Boolean,
        preferredQuality: String = ""
    ): File {
        val playlistUrl: String
        val playlistText: String

        val rootText = fetchText(m3u8Url, headers)
        if (HlsParser.isMaster(rootText)) {
            val variants = HlsParser.variants(m3u8Url, rootText)
            val preferred = preferredQuality.ifBlank { settingsManager.getSettings().downloadQuality }
            val picked = HlsParser.pick(variants, preferred) ?: variants.first()
            playlistUrl = picked.url
            playlistText = fetchText(playlistUrl, headers)
        } else {
            playlistUrl = m3u8Url
            playlistText = rootText
        }

        val segments = HlsParser.segments(playlistUrl, playlistText)
        if (segments.isEmpty()) throw IllegalStateException("Playlist contained no segments")

        targetDir.mkdirs()
        val outFile = File(targetDir, "${safeTitle}_Ep${episodeNumber}.ts")
        outFile.delete()

        FileOutputStream(outFile, true).use { output ->
            segments.forEachIndexed { index, segmentUrl ->
                currentCoroutineContext().ensureActive()
                val response = httpClient.newCall(request(segmentUrl, headers)).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("Segment ${index + 1} HTTP ${resp.code}")
                    resp.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            output.write(buffer, 0, read)
                        }
                    }
                }
                val pct = (((index + 1) * 100) / segments.size).coerceIn(0, 99)
                updateProgress(downloadId, pct)
                database.downloadDao().updateProgress(downloadId, pct, STATUS_DOWNLOADING, formatFileSize(outFile.length()))
                if (notify && (index % 5 == 0 || index == segments.size - 1)) {
                    postProgressNotification(downloadId.hashCode(), anime.title, episodeNumber, pct)
                }
            }
        }
        Log.i(TAG, "HLS download complete: ${outFile.name} (${segments.size} segments)")
        return outFile
    }

    private suspend fun downloadProgressive(
        url: String,
        targetDir: File,
        safeTitle: String,
        episodeNumber: Int,
        headers: Map<String, String>,
        downloadId: String,
        anime: Anime,
        notify: Boolean
    ): File {
        targetDir.mkdirs()
        val extension = when {
            url.contains(".ts") -> "ts"
            url.contains(".mkv") -> "mkv"
            else -> "mp4"
        }
        val outFile = File(targetDir, "${safeTitle}_Ep${episodeNumber}.$extension")

        httpClient.newCall(request(url, headers)).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} fetching stream")
            val body = response.body ?: throw IllegalStateException("Empty response body")
            val total = body.contentLength()
            FileOutputStream(outFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(128 * 1024)
                    var copied = 0L
                    var read: Int
                    var lastUpdate = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        currentCoroutineContext().ensureActive()
                        output.write(buffer, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 600) {
                            lastUpdate = now
                            val pct = if (total > 0) ((copied * 100) / total).toInt().coerceIn(0, 99) else 0
                            updateProgress(downloadId, pct)
                            database.downloadDao().updateProgress(downloadId, pct, STATUS_DOWNLOADING, formatFileSize(copied))
                            if (notify) postProgressNotification(downloadId.hashCode(), anime.title, episodeNumber, pct)
                        }
                    }
                }
            }
        }
        return outFile
    }

    private fun fetchText(url: String, headers: Map<String, String>): String {
        httpClient.newCall(request(url, headers)).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} fetching playlist")
            return response.body?.string() ?: throw IllegalStateException("Empty playlist")
        }
    }

    /**
     * Saves the subtitle track the player captured for this episode.
     * Nothing is fabricated: when no real track was captured no file is written.
     */
    private fun writeSubtitle(
        downloadId: String,
        headers: Map<String, String>,
        targetDir: File,
        safeTitle: String,
        episodeNumber: Int
    ) {
        try {
            val track = SUBTITLE_CACHE[downloadId] ?: return
            val rawBody = fetchText(track.url, headers)
            // The subtitle delay preference is applied to the saved file's timestamps.
            val shifted = shiftCues(rawBody, settingsManager.getSettings().subtitleDelayMs)
            File(targetDir, "${safeTitle}_Ep${episodeNumber}.vtt").writeText(shifted)
            Log.i(TAG, "subtitle saved for $downloadId")
        } catch (t: Throwable) {
            Log.w(TAG, "subtitle save skipped: ${t.message}")
        }
    }

    /** Shifts every `HH:MM:SS.mmm --> HH:MM:SS.mmm` cue in a WebVTT file by [offsetMs]. */
    private fun shiftCues(body: String, offsetMs: Int): String {
        if (offsetMs == 0) return body
        val pattern = Regex("(\\d{2}):(\\d{2}):(\\d{2})[.,](\\d{3})")
        return pattern.replace(body) { match ->
            val hours = match.groupValues[1].toInt()
            val minutes = match.groupValues[2].toInt()
            val seconds = match.groupValues[3].toInt()
            val millis = match.groupValues[4].toInt()
            var total = ((hours * 3600 + minutes * 60 + seconds) * 1000 + millis) + offsetMs
            if (total < 0) total = 0
            String.format(
                java.util.Locale.US,
                "%02d:%02d:%02d.%03d",
                total / 3600000,
                (total % 3600000) / 60000,
                (total % 60000) / 1000,
                total % 1000
            )
        }
    }

    /** Copies a finished file into the public Downloads collection so other players see it. */
    fun exportToPublicFolder(file: File, safeTitle: String): Boolean {
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, if (file.extension == "ts") "video/mp2t" else "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Zenith-Anime/$safeTitle")
            }
            val uri = context.contentResolver.insert(collection, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                return true
            }
            // Fall back to the legacy public folder (pre-scoped storage devices).
            val legacyDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Zenith-Anime/$safeTitle"
            ).apply { mkdirs() }
            val target = File(legacyDir, file.name)
            file.copyTo(target, overwrite = true)
            scan(target)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "export failed: ${t.message}")
            false
        }
    }

    /** Copies a finished episode into the folder the user picked with the system folder picker. */
    fun copyToSafFolder(file: File, treeUri: String): Boolean {
        return try {
            val tree = Uri.parse(treeUri)
            val parentId = android.provider.DocumentsContract.getTreeDocumentId(tree)
            val parentDoc = android.provider.DocumentsContract.buildDocumentUriUsingTree(tree, parentId)
            val created = android.provider.DocumentsContract.createDocument(
                context.contentResolver,
                parentDoc,
                if (file.extension == "ts") "video/mp2t" else "video/mp4",
                file.name
            ) ?: return false
            context.contentResolver.openOutputStream(created)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            true
        } catch (t: Throwable) {
            Log.w(TAG, "SAF copy failed: ${t.message}")
            false
        }
    }

    private fun appDownloadDirectory(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        return File(base, "Zenith-Anime")
    }

    private fun targetDirectory(safeTitle: String): File {
        return File(appDownloadDirectory(), safeTitle)
    }

    /** Cancels all active downloads and removes all downloaded files from disk. */
    fun clearAllDownloadFiles() {
        activeJobs.keys.toList().forEach { id ->
            activeJobs.remove(id)?.cancel()
            cancelNotification(id.hashCode())
        }
        _downloadProgress.value = emptyMap()
        try {
            appDownloadDirectory().takeIf { it.exists() }?.deleteRecursively()
        } catch (t: Throwable) {
            Log.w(TAG, "clearAllDownloadFiles failed: ${t.message}")
        }
    }

    private fun scan(file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(if (file.extension == "ts") "video/mp2t" else "video/mp4"),
                null
            )
        } catch (_: Throwable) {
        }
    }

    private fun updateProgress(id: String, pct: Int) {
        _downloadProgress.update { current -> current + (id to pct) }
    }

    suspend fun deleteDownload(id: String) = withContext(Dispatchers.IO) {
        activeJobs.remove(id)?.cancel()
        val item = database.downloadDao().getDownloadById(id)
        if (item != null && item.filePath.isNotBlank()) {
            File(item.filePath).takeIf { it.exists() }?.delete()
        }
        // The subtitle sidecar lives beside the video, so deleting only the video used to leave a
        // stray .vtt on disk for every download the user removed.
        item?.let { subtitleSidecarFor(it)?.delete() }
        database.downloadDao().deleteDownload(id)
        _downloadProgress.update { current -> current - id }
        cancelNotification(id.hashCode())
    }

    /** Used by "delete after watching": removes the offline file for an episode. */
    suspend fun deleteByEpisode(animeId: String, episodeNumber: Int) = withContext(Dispatchers.IO) {
        val id = "${animeId}_$episodeNumber"
        val item = database.downloadDao().getDownloadById(id) ?: return@withContext
        if (item.status != STATUS_COMPLETED) return@withContext
        deleteDownload(id)
    }

    /** Absolute path of a finished download, used to play offline without touching the network. */
    suspend fun localFileFor(animeId: String, episodeNumber: Int): String? = withContext(Dispatchers.IO) {
        val id = "${animeId}_$episodeNumber"
        val item = database.downloadDao().getDownloadById(id) ?: return@withContext null
        if (item.status != STATUS_COMPLETED || item.filePath.isBlank()) return@withContext null
        val file = File(item.filePath)
        if (!file.exists() || file.length() <= 0L) return@withContext null
        file.absolutePath
    }

    /**
     * The subtitle sidecar saved beside a finished download, if one was captured.
     * Offline playback used to ignore these files completely, so a downloaded episode played with no
     * captions even though the text was sitting next to the video.
     */
    suspend fun localSubtitleFor(animeId: String, episodeNumber: Int): String? = withContext(Dispatchers.IO) {
        val item = database.downloadDao().getDownloadById("${animeId}_$episodeNumber") ?: return@withContext null
        if (item.status != STATUS_COMPLETED) return@withContext null
        subtitleSidecarFor(item)?.takeIf { it.length() > 0L }?.absolutePath
    }

    /**
     * Locates the subtitle file written next to a download's video. Shared with deletion so removing
     * a download takes its captions with it.
     */
    private fun subtitleSidecarFor(item: DownloadItem): File? {
        if (item.filePath.isBlank()) return null
        val directory = File(item.filePath).parentFile ?: return null
        if (!directory.isDirectory) return null

        val safeTitle = item.animeTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(40)
        val expected = File(directory, "${safeTitle}_Ep${item.episodeNumber}.vtt")
        if (expected.isFile) return expected

        // The title may have been renamed since the download; any sidecar for this episode will do.
        return directory.listFiles()
            ?.firstOrNull { it.isFile && it.name.endsWith(".vtt", true) && it.name.contains("_Ep${item.episodeNumber}.") }
    }

    private fun postProgressNotification(notifId: Int, title: String, epNumber: Int, progress: Int) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_zenith_logo)
                .setContentTitle("Downloading: $title")
                .setContentText("Episode $epNumber • $progress%")
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: Throwable) {
        }
    }

    private fun postCompleteNotification(notifId: Int, title: String, epNumber: Int) {
        try {
            val playSound = settingsManager.getSettings().downloadNotificationsSound
            val channelId = if (playSound) CHANNEL_SOUND_ID else CHANNEL_ID
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_zenith_logo)
                .setContentTitle("Download complete")
                .setContentText("$title • Episode $epNumber is ready to watch offline")
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setSilent(!playSound)
                .setPriority(
                    if (playSound) NotificationCompat.PRIORITY_DEFAULT
                    else NotificationCompat.PRIORITY_LOW
                )
            if (playSound) {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            }
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: Throwable) {
        }
    }

    private fun cancelNotification(notifId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (_: Throwable) {
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 1000) {
            String.format(java.util.Locale.US, "%.2f GB", mb / 1024)
        } else {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        }
    }

}
