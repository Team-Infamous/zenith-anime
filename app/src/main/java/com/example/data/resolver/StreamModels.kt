package com.example.data.resolver

import com.example.data.model.Anime
import com.example.data.model.Episode
import okhttp3.OkHttpClient
import okhttp3.Request

/** A directly playable stream discovered by a resolver. */
data class StreamSource(
    val url: String,
    val qualityLabel: String,
    val isHls: Boolean,
    val audioLanguage: String,
    val serverId: String,
    val serverName: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    /** True when this URL is a progressive file (mp4) rather than an HLS playlist. */
    val isProgressive: Boolean = false,
    /** Audio renditions discovered inside an HLS master playlist (`#EXT-X-MEDIA:TYPE=AUDIO`). */
    val audioTracks: List<AudioTrackOption> = emptyList()
) {
    val key: String get() = "$serverId|$qualityLabel|$url"
}

data class SubtitleTrack(
    val url: String,
    val label: String,
    val language: String,
    val isDefault: Boolean = false
)

/** An audio rendition discovered in an HLS master playlist or stream metadata. */
data class AudioTrackOption(
    val id: String,
    val label: String,
    val language: String,
    val uri: String? = null,
    val isDub: Boolean = false,
    val isDefault: Boolean = false,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1
)

/** One resolver's outcome, including why it failed so the UI can explain itself. */
data class ResolveAttempt(
    val resolverId: String,
    val resolverName: String,
    val sources: List<StreamSource>,
    val error: String? = null,
    val availableAudioLanguages: Set<String> = emptySet(),
    val dubUnavailable: Boolean = false
)

data class ResolveResult(
    val attempts: List<ResolveAttempt>,
    val queryTitle: String,
    val requestedLanguage: String = "sub"
) {
    val sources: List<StreamSource> = attempts.flatMap { it.sources }
    val hasAnySource: Boolean get() = sources.isNotEmpty()
    val availableAudioLanguages: Set<String> = attempts.flatMap { it.availableAudioLanguages }.toSet()
    val isDubUnavailable: Boolean
        get() = requestedLanguage.equals("dub", ignoreCase = true) &&
            !hasAnySource &&
            (attempts.any { it.dubUnavailable } || ("sub" in availableAudioLanguages && "dub" !in availableAudioLanguages))

    /**
     * Human readable, consumer-facing explanation of why playback could not start.
     *
     * Inert whenever a stream was found: an attempt that failed while another server succeeded is
     * not a reason to tell the user anything went wrong.
     */
    fun failureSummary(): String {
        if (hasAnySource) return "No playable stream found."
        if (isDubUnavailable) {
            return "English Dub audio is not available for this episode yet. Subtitled (SUB) audio is available."
        }
        if (attempts.isEmpty()) return "No servers were available for this episode."
        val failures = attempts.filter { it.sources.isEmpty() }
        if (failures.isEmpty()) return "No playable stream found."
        return failures.joinToString("\n") { attempt ->
            val reason = attempt.error?.takeIf { it.isNotBlank() } ?: "No stream returned"
            "• ${attempt.resolverName}: $reason"
        }
    }
}

/**
 * Resolves real, downloadable stream URLs for an episode.
 * Implementations are ports of the scraping logic used by the Aniyomi extensions installed on the
 * user's device, so they return HLS playlists with quality variants plus subtitle tracks.
 */
interface AnimeStreamResolver {
    val id: String
    val displayName: String
    /** True when this resolver talks to a third-party site and may be blocked or offline. */
    val isRemote: Boolean

    suspend fun resolve(anime: Anime, episode: Episode, language: String): AnimeStreamResolver.Output

    data class Output(
        val sources: List<StreamSource>,
        val subtitles: List<SubtitleTrack> = emptyList(),
        val error: String? = null,
        val availableAudioLanguages: Set<String> = emptySet(),
        val dubUnavailable: Boolean = false
    )
}

/** Shared HTTP helpers: every resolver needs the same headers and error handling. */
internal object Http {
    const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 15; SM-S166V) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    fun get(client: OkHttpClient, url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val builder = Request.Builder().url(url)
            if (headers.none { it.key.equals("User-Agent", true) }) {
                builder.header("User-Agent", DEFAULT_UA)
            }
            headers.forEach { (k, v) -> if (v.isNotBlank()) builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * These sites wrap HTML and URLs inside JSON, so payloads arrive escaped (`"key"`,
     * `https:\/\/host\/a`). Unescape before pattern matching, or every extracted URL keeps its
     * backslashes and relative-path derivation lands one segment too deep.
     */
    fun unescapeJson(value: String): String = value
        .replace("\\n", " ")
        .replace("\\/", "/")
        .replace("\\\"", "\"")

    fun jsonField(raw: String, field: String): String? {
        return Regex("\"$field\"\\s*:\\s*\"(.*?)\"").find(raw)?.groupValues?.getOrNull(1)
    }
}
