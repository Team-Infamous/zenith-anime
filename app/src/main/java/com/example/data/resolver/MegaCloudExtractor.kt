package com.example.data.resolver

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.net.URLEncoder

/**
 * Port of `aniyomi.lib.megacloudextractor.MegaCloudExtractor` recovered from the installed
 * AniWatch/Zoro extension.
 *
 * Flow: embed page -> 48 char nonce -> `/embed-2/v3/e-1/getSources?id=<id>&_k=<nonce>` -> when the
 * payload is encrypted, ask the public decryptor for the real playlist using the rotating "mega"
 * key, then expand the HLS master playlist into quality variants.
 */
class MegaCloudExtractor(
    private val client: OkHttpClient
) {
    private var cachedKey: String? = null

    data class Result(
        val sources: List<StreamSource>,
        val subtitles: List<SubtitleTrack>,
        val error: String? = null
    )

    fun extract(embedUrl: String, serverId: String, serverName: String, language: String): Result {
        val id = embedUrl.substringAfter("/e-1/", "").substringBefore("?")
        if (id.isBlank()) return Result(emptyList(), emptyList(), "Not a MegaCloud link")

        val origin = try {
            "https://" + (embedUrl.toHttpUrlOrNull()?.host
                ?: return Result(emptyList(), emptyList(), "Invalid embed host"))
        } catch (_: Throwable) {
            return Result(emptyList(), emptyList(), "Invalid embed host")
        }

        val headers = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to "$origin/"
        )

        val page = Http.get(client, embedUrl, headers)
            ?: return Result(emptyList(), emptyList(), "Embed page unreachable")

        val nonce = extractNonce(page)
            ?: return Result(emptyList(), emptyList(), "No nonce in embed page")

        val raw = Http.get(client, "$origin/embed-2/v3/e-1/getSources?id=$id&_k=$nonce", headers)
            ?: return Result(emptyList(), emptyList(), "getSources failed")

        // The payload JSON-escapes its URLs (`https:\/\/...`), so unescape before parsing or
        // relative-looking URLs are produced.
        val payload = Http.unescapeJson(raw)

        val fileUrls = Regex("\"file\"\\s*:\\s*\"(.*?)\"").findAll(payload).map { it.groupValues[1] }.toList()
        if (fileUrls.isEmpty()) return Result(emptyList(), emptyList(), "No sources in response")

        val encrypted = payload.contains("\"encrypted\":true")
        val subtitles = parseTracks(payload)

        val playlists = fileUrls.mapNotNull { file ->
            var url = file
            if (encrypted && !url.contains(".m3u8")) {
                url = decrypt(file, nonce) ?: return@mapNotNull null
            }
            url
        }

        if (playlists.isEmpty()) return Result(emptyList(), emptyList(), "Could not decrypt stream URL")

        val sources = mutableListOf<StreamSource>()
        for (playlist in playlists) {
            val (variants, audioTracks) = expandVariantsAndAudio(playlist, origin)
            if (variants.isEmpty()) {
                sources += StreamSource(
                    url = playlist,
                    qualityLabel = "Auto",
                    isHls = playlist.contains(".m3u8"),
                    audioLanguage = language,
                    serverId = serverId,
                    serverName = serverName,
                    headers = headers,
                    subtitles = subtitles,
                    isProgressive = !playlist.contains(".m3u8"),
                    audioTracks = audioTracks
                )
            } else {
                sources += StreamSource(
                    url = playlist,
                    qualityLabel = "Auto",
                    isHls = true,
                    audioLanguage = language,
                    serverId = serverId,
                    serverName = "$serverName Auto",
                    headers = headers,
                    subtitles = subtitles,
                    audioTracks = audioTracks
                )
                variants.forEach { variant ->
                    sources += StreamSource(
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
        }

        Log.i(TAG, "extracted ${sources.size} source(s) + ${subtitles.size} subtitle(s) from $serverName")
        return Result(sources, subtitles)
    }

    private fun extractNonce(page: String): String? {
        Regex("\\b[a-zA-Z0-9]{48}\\b").find(page)?.let { return it.value }
        val triple = Regex(
            "\\b([a-zA-Z0-9]{16})\\b[\\s\\S]*?\\b([a-zA-Z0-9]{16})\\b[\\s\\S]*?\\b([a-zA-Z0-9]{16})\\b"
        ).find(page) ?: return null
        return triple.groupValues[1] + triple.groupValues[2] + triple.groupValues[3]
    }

    private fun decrypt(encryptedData: String, nonce: String): String? {
        val key = cachedKey ?: fetchKey()?.also { cachedKey = it } ?: return null
        val url = "$DECRYPT_ENDPOINT?encrypted_data=" + URLEncoder.encode(encryptedData, "UTF-8") +
            "&nonce=" + URLEncoder.encode(nonce, "UTF-8") +
            "&secret=" + URLEncoder.encode(key, "UTF-8")
        val body = Http.get(client, url) ?: return null
        return Http.jsonField(body, "file") ?: Http.jsonField(body, "url")
    }

    private fun fetchKey(): String? {
        val body = Http.get(client, KEYS_URL) ?: return null
        return Http.jsonField(body, "mega")
    }

    private fun parseTracks(raw: String): List<SubtitleTrack> {
        val out = mutableListOf<SubtitleTrack>()
        val trackRegex = Regex(
            "\\{[^{}]*\"kind\"\\s*:\\s*\"captions\"[^{}]*\\}", RegexOption.IGNORE_CASE
        )
        for (match in trackRegex.findAll(raw)) {
            val block = match.value
            val file = Http.jsonField(block, "file") ?: continue
            val label = Http.jsonField(block, "label") ?: "Subtitles"
            val lang = Http.jsonField(block, "lang") ?: label
            out += SubtitleTrack(url = file, label = label, language = lang)
        }
        if (out.isNotEmpty()) return out

        // Fallback: some responses only expose the tracks array with "url" keys.
        val urlRegex = Regex("\"kind\"\\s*:\\s*\"captions\"[\\s\\S]{0,400}?\"(?:file|url)\"\\s*:\\s*\"(.*?)\"")
        urlRegex.findAll(raw).forEach { match ->
            out += SubtitleTrack(url = match.groupValues[1], label = "Subtitles", language = "und")
        }
        return out
    }

    /** Expands an HLS master playlist into quality variants and audio rendition tracks. */
    private fun expandVariantsAndAudio(
        playlistUrl: String,
        origin: String
    ): Pair<List<HlsParser.Variant>, List<AudioTrackOption>> {
        if (!playlistUrl.contains(".m3u8")) return emptyList<HlsParser.Variant>() to emptyList()
        val headers = mapOf("Referer" to "$origin/", "Accept" to "*/*")
        val body = Http.get(client, playlistUrl, headers) ?: return emptyList<HlsParser.Variant>() to emptyList()
        if (!HlsParser.isMaster(body)) return emptyList<HlsParser.Variant>() to emptyList()
        return HlsParser.variants(playlistUrl, body) to HlsParser.audioTracks(playlistUrl, body)
    }

    private companion object {
        const val TAG = "ZenithResolver"
        const val KEYS_URL =
            "https://raw.githubusercontent.com/yogesh-hacker/MegacloudKeys/refs/heads/main/keys.json"
        const val DECRYPT_ENDPOINT =
            "https://script.google.com/macros/s/AKfycbxHbYHbrGMXYD2-bC-C43D3njIbU-wGiYQuJL61H4vyy6YVXkybMNNEPJNPPuZrD1gRVA/exec"
    }
}
