package com.example.data.resolver

import android.util.Log
import com.example.data.model.Anime
import com.example.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Port of the Aniyomi `AllAnime` extension
 * (`eu.kanade.tachiyomi.animeextension.en.allanime.AllAnime`) recovered from the extension APK
 * installed on the device.
 *
 * Recovered from the decompiled sources:
 *  - the GraphQL documents in [AllAnimeQueries] (`AllAnimeQueriesKt`);
 *  - the API contract: `POST {apiUrl}/api` with a `{"variables": {...}, "query": "<document>"}`
 *    body and the `Origin` / `Referer` headers built by `AllAnime.buildPost` and
 *    `AllAnime.videoListRequest`;
 *  - `AllAnime.decryptSource`, the only obfuscation step the extension applies to a source url:
 *    values prefixed with `-`/`--` are hex pairs, each byte XOR 0x38 (56);
 *  - the `VideoLink` / `Link` / `Subtitles` shapes used when a decoded source url is itself JSON.
 *
 * Deliberately NOT guessed at: `AllAnime.getVideoList` and `AllAnimeExtractor.videoFromUrl` were
 * both reported as "Method not decompiled" in the extension, so the per-hoster iframe extraction
 * (GogoStream, Filemoon, StreamWish, Okru, Mp4Upload, Dood, Streamlare) could not be ported here.
 * A source that does not decode to a directly playable media url is reported as unsupported
 * instead of inventing a url.
 */
class AllAnimeResolver : AnimeStreamResolver {
    override val id = "allanime"
    override val displayName = "AllAnime"
    override val isRemote = true

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** `AllAnime.PREF_DOMAIN_DEFAULT` and `PREF_SITE_DOMAIN_DEFAULT` from the extension. */
    private val apiUrl = "https://api.allanime.day"
    private val siteUrl = "https://allmanga.to"

    /** `AllAnime.PAGE_SIZE`. */
    private val pageSize = 26

    /** Same headers the extension hands to `AllAnimeExtractor` for playback requests. */
    private val streamHeaders = mapOf(
        "Accept" to "*/*",
        "Referer" to "$apiUrl/",
        "Origin" to siteUrl
    )

    override suspend fun resolve(
        anime: Anime,
        episode: Episode,
        language: String
    ): AnimeStreamResolver.Output = withContext(Dispatchers.IO) {
        // AllAnime calls the audio track a "translation type".
        val translation = if (language.equals("dub", true)) "dub" else "sub"
        try {
            val showId = findShowId(anime, translation)
                ?: findShowId(anime, "sub")
                ?: return@withContext fail("not found in the AllAnime catalogue")

            val epInfo = findEpisodeString(showId, episode.number, translation)
            if (epInfo.episodeString == null) {
                return@withContext if (epInfo.dubUnavailable) {
                    AnimeStreamResolver.Output(
                        sources = emptyList(),
                        error = "English Dub is not available for Episode ${episode.number} on AllAnime (Sub is available)",
                        availableAudioLanguages = epInfo.availableLanguages,
                        dubUnavailable = true
                    )
                } else {
                    AnimeStreamResolver.Output(
                        sources = emptyList(),
                        error = "episode ${episode.number} is not listed",
                        availableAudioLanguages = epInfo.availableLanguages
                    )
                }
            }
            val episodeString = epInfo.episodeString

            val links = fetchSourceLinks(showId, episodeString, translation)
            if (links.isEmpty()) {
                return@withContext AnimeStreamResolver.Output(
                    sources = emptyList(),
                    error = "no servers returned for episode ${episode.number}",
                    availableAudioLanguages = epInfo.availableLanguages
                )
            }

            val sources = mutableListOf<StreamSource>()
            val subtitles = mutableListOf<SubtitleTrack>()
            val unsupported = mutableListOf<String>()

            for (link in links) {
                // Only real media urls are usable; an iframe needs a hoster extractor we do not have.
                val url = decodeIfNeeded(link.url)
                if (!url.startsWith("http")) {
                    unsupported += link.name
                    continue
                }
                val isHls = link.isHls || url.contains(".m3u8")
                val isProgressive = link.isProgressive || url.contains(".mp4")
                if (!isHls && !isProgressive) {
                    unsupported += link.name
                    continue
                }

                subtitles += link.subtitles
                sources += StreamSource(
                    url = url,
                    qualityLabel = link.resolution.ifBlank { "Auto" },
                    isHls = isHls,
                    audioLanguage = translation,
                    serverId = id,
                    serverName = link.name,
                    headers = streamHeaders,
                    subtitles = link.subtitles,
                    isProgressive = isProgressive
                )
            }

            if (sources.isEmpty()) {
                val hosters = unsupported.distinct().take(3).joinToString(", ")
                return@withContext AnimeStreamResolver.Output(
                    sources = emptyList(),
                    error = if (hosters.isBlank()) {
                        "no playable source returned"
                    } else {
                        "server needs per-host extraction ($hosters)"
                    },
                    availableAudioLanguages = epInfo.availableLanguages
                )
            }

            Log.i(TAG, "ep ${episode.number} -> ${sources.size} source(s) via AllAnime")
            AnimeStreamResolver.Output(
                sources = sources.distinctBy { it.key },
                subtitles = subtitles.distinctBy { it.url },
                availableAudioLanguages = epInfo.availableLanguages
            )
        } catch (t: Throwable) {
            Log.w(TAG, "allanime failed: ${t.javaClass.simpleName}: ${t.message}")
            fail(t.javaClass.simpleName)
        }
    }

    private fun fail(reason: String) = AnimeStreamResolver.Output(emptyList(), error = reason)

    /** `AllAnime.buildPost` / `videoListRequest`: POST the query body to `{apiUrl}/api`. */
    private fun post(body: JSONObject): JSONObject? {
        val request = Request.Builder()
            .url("$apiUrl/api")
            .post(body.toString().toRequestBody(jsonMediaType))
            .header("Accept", "*/*")
            .header("Origin", siteUrl)
            .header("Referer", "$apiUrl/")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string()
                if (!response.isSuccessful || text.isNullOrBlank()) null else JSONObject(text)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "allanime request failed: ${t.javaClass.simpleName}: ${t.message}")
            null
        }
    }

    /** SEARCH_QUERY -> `data.shows.edges`, matched on the title the app already shows. */
    private fun findShowId(anime: Anime, translation: String): String? {
        val searchQueries = listOf(anime.title, anime.englishTitle, anime.romajiTitle)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val wanted = listOf(anime.title, anime.englishTitle, anime.romajiTitle, anime.nativeTitle)
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }

        var firstFallbackId: String? = null
        for (queryTitle in searchQueries) {
            val root = post(
                JSONObject().apply {
                    put(
                        "variables",
                        JSONObject().apply {
                            put(
                                "search",
                                JSONObject().apply {
                                    put("allowAdult", true)
                                    put("allowUnknown", true)
                                    put("query", queryTitle)
                                }
                            )
                            put("limit", pageSize)
                            put("page", 1)
                            put("translationType", translation)
                        }
                    )
                    put("query", AllAnimeQueries.SEARCH)
                }
            ) ?: continue

            val edges = root.optJSONObject("data")
                ?.optJSONObject("shows")
                ?.optJSONArray("edges")
                ?: continue

            for (i in 0 until edges.length()) {
                val edge = edges.optJSONObject(i) ?: continue
                val names = listOf(
                    edge.optString("englishName"),
                    edge.optString("name"),
                    edge.optString("nativeName")
                ).map { it.lowercase().trim() }

                val showId = edge.optString("_id").trim()
                if (showId.isEmpty()) continue
                if (wanted.any { title -> names.any { it == title } }) return showId
            }

            if (firstFallbackId == null) {
                firstFallbackId = edges.optJSONObject(0)?.optString("_id")?.trim()?.takeIf { it.isNotEmpty() }
            }
        }

        // The extension takes the first result when nothing matches exactly.
        return firstFallbackId
    }

    private data class EpisodeLookup(
        val episodeString: String?,
        val availableLanguages: Set<String>,
        val dubUnavailable: Boolean
    )

    internal fun parseAvailableEpisodeDetail(
        detail: JSONObject,
        number: Int,
        translation: String
    ): Pair<String?, Boolean> {
        val lookup = matchEpisodeInDetail(detail, number, translation)
        return lookup.episodeString to lookup.dubUnavailable
    }

    private fun matchEpisodeInDetail(
        detail: JSONObject,
        number: Int,
        translation: String
    ): EpisodeLookup {
        val exact = number.toString()
        fun findInArray(arr: JSONArray?): String? {
            if (arr == null) return null
            for (i in 0 until arr.length()) {
                val value = arr.optString(i).trim()
                if (value == exact) return value
            }
            for (i in 0 until arr.length()) {
                val value = arr.optString(i).trim()
                if (value.toFloatOrNull()?.toInt() == number) return value
            }
            return null
        }

        val subMatch = findInArray(detail.optJSONArray("sub"))
        val dubMatch = findInArray(detail.optJSONArray("dub"))
        val available = buildSet {
            if (subMatch != null) add("sub")
            if (dubMatch != null) add("dub")
        }

        val wantedMatch = if (translation == "dub") dubMatch else subMatch
        val dubMissing = translation == "dub" && dubMatch == null && subMatch != null
        return EpisodeLookup(
            episodeString = wantedMatch,
            availableLanguages = available,
            dubUnavailable = dubMissing
        )
    }

    /** EPISODES_QUERY -> `data.show.availableEpisodesDetail[sub|dub]`. */
    private fun findEpisodeString(showId: String, number: Int, translation: String): EpisodeLookup {
        val root = post(
            JSONObject().apply {
                put("variables", JSONObject().apply { put("_id", showId) })
                put("query", AllAnimeQueries.EPISODES)
            }
        ) ?: return EpisodeLookup(null, emptySet(), false)

        val detail = root.optJSONObject("data")
            ?.optJSONObject("show")
            ?.optJSONObject("availableEpisodesDetail")
            ?: return EpisodeLookup(null, emptySet(), false)

        return matchEpisodeInDetail(detail, number, translation)
    }

    /** STREAMS_QUERY -> `data.episode.sourceUrls`. */
    private fun fetchSourceLinks(
        showId: String,
        episodeString: String,
        translation: String
    ): List<RawLink> {
        val root = post(
            JSONObject().apply {
                put(
                    "variables",
                    JSONObject().apply {
                        put("showId", showId)
                        put("translationType", translation)
                        put("episodeString", episodeString)
                    }
                )
                put("query", AllAnimeQueries.STREAMS)
            }
        ) ?: return emptyList()

        val payload = root.optJSONObject("data")
            ?.optJSONObject("episode")
            ?.opt("sourceUrls")
            ?: return emptyList()

        return parsePayload(payload)
    }

    /**
     * `sourceUrls` is an array of `SourceUrl` objects in the decompiled DTO, but the same endpoint
     * has also returned it as an embedded JSON string, so both shapes are accepted.
     */
    private fun parsePayload(payload: Any): List<RawLink> {
        val text = when (payload) {
            is JSONArray -> null
            is JSONObject -> null
            is String -> payload.trim()
            else -> return emptyList()
        }

        if (text != null) {
            if (text.isEmpty()) return emptyList()
            return try {
                when {
                    text.startsWith("[") -> parseArray(JSONArray(text))
                    text.startsWith("{") -> linksOf(JSONObject(text))
                    else -> listOf(RawLink(text, "AllAnime", "", false, false, emptyList()))
                }
            } catch (_: Throwable) {
                emptyList()
            }
        }

        return when (payload) {
            is JSONArray -> parseArray(payload)
            is JSONObject -> linksOf(payload)
            else -> emptyList()
        }
    }

    private fun parseArray(array: JSONArray): List<RawLink> {
        val out = mutableListOf<RawLink>()
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { out += linksOf(it) }
        }
        return out
    }

    /** Accepts either a `VideoLink` (`{"links": [...]}`) or a single `Link` / `SourceUrl`. */
    private fun linksOf(obj: JSONObject): List<RawLink> {
        obj.optJSONArray("links")?.let { return parseArray(it) }
        return listOfNotNull(linkFrom(obj))
    }

    private fun linkFrom(obj: JSONObject): RawLink? {
        val url = obj.optString("link").trim().takeIf { it.isNotEmpty() }
            ?: obj.optString("sourceUrl").trim().takeIf { it.isNotEmpty() }
            ?: return null

        val type = obj.optString("type").lowercase()
        return RawLink(
            url = url,
            name = obj.optString("sourceName").trim().takeIf { it.isNotEmpty() }
                ?: obj.optString("resolutionStr").trim().takeIf { it.isNotEmpty() }
                ?: "AllAnime",
            resolution = obj.optString("resolutionStr").trim(),
            isHls = obj.optBoolean("hls", false) || type.contains("hls"),
            isProgressive = obj.optBoolean("mp4", false) ||
                type.contains("mp4") ||
                type.contains("raw"),
            subtitles = parseSubtitles(obj.optJSONArray("subtitles"))
        )
    }

    /** `VideoLink.Link.Subtitles { lang, src, label }`; only absolute urls are usable. */
    private fun parseSubtitles(array: JSONArray?): List<SubtitleTrack> {
        if (array == null) return emptyList()
        val out = mutableListOf<SubtitleTrack>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val src = item.optString("src").trim()
            if (!src.startsWith("http")) continue
            val language = item.optString("lang").trim().takeIf { it.isNotEmpty() } ?: "und"
            out += SubtitleTrack(
                url = src,
                label = item.optString("label").trim().takeIf { it.isNotEmpty() } ?: language,
                language = language
            )
        }
        return out
    }

    /**
     * Port of `AllAnime.decryptSource`. Source urls are obfuscated by hex-encoding every character
     * XOR 0x38 and prefixing the payload with `-` or `--`; the original strips everything up to and
     * including the last dash, then decodes two hex digits at a time.
     */
    internal fun decodeIfNeeded(value: String): String {
        val trimmed = value.trim()
        val decoded = if (trimmed.startsWith("-")) {
            val payload = trimmed.substringAfterLast('-')
            if (payload.isEmpty() || payload.length % 2 != 0) {
                trimmed
            } else {
                val bytes = ByteArray(payload.length / 2)
                var index = 0
                var valid = true
                while (index < payload.length) {
                    val byte = payload.substring(index, index + 2).toIntOrNull(16)
                    if (byte == null) {
                        valid = false
                        break
                    }
                    bytes[index / 2] = (byte xor 0x38).toByte()
                    index += 2
                }
                if (valid) bytes.toString(Charsets.UTF_8) else trimmed
            }
        } else {
            trimmed
        }

        return when {
            decoded.startsWith("/apivtwo/clock?id=") ->
                "https://api.allanime.day/apivtwo/clock.json?id=" + decoded.removePrefix("/apivtwo/clock?id=")
            decoded.startsWith("https://api.allanime.day/apivtwo/clock?id=") ->
                "https://api.allanime.day/apivtwo/clock.json?id=" + decoded.removePrefix("https://api.allanime.day/apivtwo/clock?id=")
            else -> decoded
        }
    }

    private data class RawLink(
        val url: String,
        val name: String,
        val resolution: String,
        val isHls: Boolean,
        val isProgressive: Boolean,
        val subtitles: List<SubtitleTrack>
    )

    private companion object {
        const val TAG = "ZenithResolver"
    }
}
