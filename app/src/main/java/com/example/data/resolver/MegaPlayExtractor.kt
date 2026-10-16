package com.example.data.resolver

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Extractor for the MegaPlay player (`megaplay.buzz`) that the Anikoto/HiAnime family embeds.
 *
 * The player page is a plain HTML shell, so unlike MegaCloud this needs no client-side crypto:
 *
 * 1. GET the embed page -> the `#megaplay-player` div carries the numeric file id in `data-id`.
 * 2. GET `/stream/getSources?id=<fileId>` (AJAX header required, otherwise 403) -> subtitle
 *    tracks. The response also carries an encrypted `enc` blob, which is deliberately ignored.
 * 3. The CDN lays each file out as `<dir>/subtitles/<track>.vtt` and `<dir>/master.m3u8`, so the
 *    media directory is derived from a track URL and the master playlist is read from there.
 * 4. The master playlist is expanded into one source per quality variant.
 *
 * Every failure returns a short reason instead of guessing a URL.
 */
class MegaPlayExtractor(
    private val client: OkHttpClient
) {

    fun extract(
        embedUrl: String,
        referer: String,
        serverId: String,
        serverName: String,
        language: String
    ): AnimeStreamResolver.Output {
        val host = embedUrl.toHttpUrlOrNull()?.host
            ?: return fail("Not a valid player link")
        val origin = "https://$host"

        val pageHeaders = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to referer
        )

        val page = fetch(embedUrl, pageHeaders)
            ?: return fail("Player page did not load")

        val fileId = playerFileId(page)
            ?: return fail("Player file id missing")

        // getSources only answers real AJAX requests, and it wants the player's own origin.
        val apiHeaders = pageHeaders + ("Referer" to "$origin/")
        val raw = fetch("$origin/stream/getSources?id=$fileId", apiHeaders)
            ?: return fail("Could not read stream info")

        // The payload is JSON-escaped: `\"` around keys and `\/` inside every URL. Unescape first,
        // otherwise the media directory is derived one path segment too deep.
        val unescaped = Http.unescapeJson(raw)
        val subtitles = parseTracks(unescaped)
        var mediaDir = subtitles.firstNotNullOfOrNull { mediaDirOf(it.url) }
        if (mediaDir == null) {
            val anyTrackUrl = Regex("\"file\"\\s*:\\s*\"(https?://[^\"]+)\"").find(unescaped)?.groupValues?.getOrNull(1)
            mediaDir = anyTrackUrl?.let { mediaDirOf(it) }
        }
        if (mediaDir == null) return fail("Stream location not found")

        val masterUrl = "$mediaDir/$MASTER_PLAYLIST"
        Log.i(TAG, "megaplay file $fileId -> $masterUrl")
        val master = fetch(masterUrl, apiHeaders)
            ?: return fail("Stream playlist unreachable")
        if (!master.trimStart().startsWith("#EXTM3U")) return fail("Stream playlist invalid")

        val headers = mapOf("Referer" to "$origin/", "Accept" to "*/*")
        val variants = HlsParser.variants(masterUrl, master)
        val audioTracks = HlsParser.audioTracks(masterUrl, master)

        val sources = if (variants.isEmpty()) {
            listOf(
                StreamSource(
                    url = masterUrl,
                    qualityLabel = "Auto",
                    isHls = true,
                    audioLanguage = language,
                    serverId = serverId,
                    serverName = serverName,
                    headers = headers,
                    subtitles = subtitles,
                    audioTracks = audioTracks
                )
            )
        } else {
            val autoMaster = StreamSource(
                url = masterUrl,
                qualityLabel = "Auto",
                isHls = true,
                audioLanguage = language,
                serverId = serverId,
                serverName = "$serverName Auto",
                headers = headers,
                subtitles = subtitles,
                audioTracks = audioTracks
            )
            listOf(autoMaster) + variants.map { variant ->
                StreamSource(
                    url = variant.url,
                    qualityLabel = variant.label,
                    isHls = true,
                    audioLanguage = language,
                    serverId = serverId,
                    serverName = "$serverName ${variant.label}",
                    headers = headers,
                    subtitles = subtitles,
                    audioTracks = audioTracks
                )
            }
        }

        Log.i(TAG, "megaplay file $fileId -> ${sources.size} source(s) + ${subtitles.size} subtitle(s)")
        return AnimeStreamResolver.Output(sources, subtitles)
    }

    /** Reads `data-id` off the `#megaplay-player` element. */
    internal fun playerFileId(page: String): String? {
        Regex("id=\"megaplay-player\"[\\s\\S]{0,400}?data-id=\"(\\d+)\"")
            .find(page)?.groupValues?.getOrNull(1)?.let { return it }
        return Regex("data-id=\"(\\d+)\"").find(page)?.groupValues?.getOrNull(1)
    }

    private fun parseTracks(raw: String): List<SubtitleTrack> {
        val out = mutableListOf<SubtitleTrack>()
        val blocks = Regex("\\{[^{}]*\"kind\"\\s*:\\s*\"captions\"[^{}]*\\}", RegexOption.IGNORE_CASE)
        for (match in blocks.findAll(raw)) {
            val block = match.value
            val file = Http.jsonField(block, "file") ?: continue
            val label = Http.jsonField(block, "label") ?: "Subtitles"
            val lang = Http.jsonField(block, "lang") ?: label
            val isDefault = block.contains("\"default\":true")
            out += SubtitleTrack(url = file, label = label, language = lang, isDefault = isDefault)
        }
        return out
    }

    /** `<dir>/subtitles/track_0_eng.vtt` -> `<dir>` */
    internal fun mediaDirOf(trackUrl: String): String? {
        if (!trackUrl.startsWith("http")) return null
        val dir = if (trackUrl.contains("/subtitles/")) {
            trackUrl.substringBefore("/subtitles/")
        } else {
            trackUrl.substringBeforeLast('/', "")
        }
        return dir.takeIf { it.isNotBlank() && it != "http:" && it != "https:" }
    }

    /** Like [Http.get] but logs why it failed, so on-device diagnosis is possible. */
    private fun fetch(url: String, headers: Map<String, String>): String? {
        return try {
            val builder = Request.Builder().url(url).header("User-Agent", Http.DEFAULT_UA)
            headers.forEach { (k, v) -> if (v.isNotBlank()) builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "megaplay: HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "megaplay: ${t.javaClass.simpleName} for $url (${t.message})")
            null
        }
    }

    private fun fail(reason: String) = AnimeStreamResolver.Output(emptyList(), error = reason)

    private companion object {
        const val TAG = "ZenithResolver"
        const val MASTER_PLAYLIST = "master.m3u8"
    }
}
