package com.example.data.resolver

import android.util.Log
import com.example.data.model.Anime
import com.example.data.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Resolver for the live modern Zoro/HiAnime family (`anikototv.to`, `hianimes.re`).
 *
 * This is the current generation of the protocol the legacy `ZoroTheme` extension spoke, verified
 * end-to-end against the live sites:
 *
 * 1. `GET /ajax/anime/search?keyword=<title>` -> `/watch/<slug>` candidates. The rendered
 *    `/search` page is useless here because its results are injected by JavaScript.
 * 2. The watch page carries the numeric id in `data-anime-id` (the slug suffix is NOT the id).
 * 3. `GET /ajax/episode/list/<animeId>` -> anchors with `data-num` and a `data-ids` token.
 * 4. `GET /ajax/server/list?servers=<data-ids>` -> `<li>` rows with `data-link-id` and the server
 *    name, grouped under `data-type="sub"` / `"dub"`.
 * 5. `GET /ajax/server?get=<data-link-id>` -> `result.url`, the real embed.
 * 6. The embed goes to whichever extractor matches it: [MegaPlayExtractor] for the megaplay.buzz
 *    player, [MegaCloudExtractor] for `/e-1/` links, [StreamTapeExtractor] for streamtape.com.
 *
 * Legitimate hosts are volatile, so candidates are ranked by title, confirmed against the AniList
 * MAL id when we have one, and every failure is reported rather than papered over.
 */
class AnikotoResolver : AnimeStreamResolver {
    override val id = "anikoto"
    override val displayName = "Anikoto"
    override val isRemote = true

    // Short timeouts keep a dead host from stalling playback; the registry falls back quickly.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val megaPlay = MegaPlayExtractor(client)
    private val megaCloud = MegaCloudExtractor(client)
    private val streamTape = StreamTapeExtractor(client, id)

    /** Verified reachable hosts, best first. */
    private val hosts = listOf(
        "https://anikototv.to",
        "https://hianimes.re"
    )

    override suspend fun resolve(
        anime: Anime,
        episode: Episode,
        language: String
    ): AnimeStreamResolver.Output = withContext(Dispatchers.IO) {
        Log.i(TAG, "anikoto: resolving '${anime.title}' ep ${episode.number} lang=$language mal=${anime.malId}")
        val failures = mutableListOf<String>()
        val discoveredAudioLanguages = mutableSetOf<String>()
        var sawDubUnavailable = false
        for (host in hosts) {
            try {
                val result = tryHost(host, anime, episode, language)
                discoveredAudioLanguages += result.availableAudioLanguages
                if (result.dubUnavailable) sawDubUnavailable = true
                if (result.sources.isNotEmpty()) {
                    Log.i(TAG, "anikoto: SUCCESS via $host -> ${result.sources.size} source(s)")
                    return@withContext result
                }
                Log.i(TAG, "anikoto: $host failed: ${result.error}")
                failures += "${host.removePrefix("https://")}: ${result.error ?: "no stream"}"
            } catch (t: Throwable) {
                Log.w(TAG, "host $host failed: ${t.javaClass.simpleName}: ${t.message}")
                failures += "${host.removePrefix("https://")}: ${t.javaClass.simpleName}"
            }
        }
        AnimeStreamResolver.Output(
            sources = emptyList(),
            error = failures.distinct().take(2).joinToString("; ").ifBlank { "No host reachable" },
            availableAudioLanguages = discoveredAudioLanguages,
            dubUnavailable = sawDubUnavailable
        )
    }

    private fun tryHost(
        host: String,
        anime: Anime,
        episode: Episode,
        language: String
    ): AnimeStreamResolver.Output {
        val referer = "$host/"
        val headers = ajaxHeaders(referer)

        val candidates = searchCandidates(host, anime, headers)
        if (candidates.isEmpty()) return fail("title not found on site")

        var lastError: String? = null
        val availableLanguages = mutableSetOf<String>()
        var dubUnavailable = false
        for (candidate in candidates.take(MAX_CANDIDATES)) {
            try {
                val result = tryCandidate(host, referer, headers, candidate, anime, episode, language)
                if (result != null) {
                    availableLanguages += result.availableAudioLanguages
                    if (result.dubUnavailable) dubUnavailable = true
                    if (result.sources.isNotEmpty()) return result
                }
                lastError = result?.error ?: "episode ${episode.number} missing"
            } catch (t: Throwable) {
                Log.w(TAG, "candidate ${candidate.path} failed: ${t.javaClass.simpleName}")
                lastError = t.javaClass.simpleName
            }
        }
        return AnimeStreamResolver.Output(
            sources = emptyList(),
            error = lastError ?: "no playable stream",
            availableAudioLanguages = availableLanguages,
            dubUnavailable = dubUnavailable
        )
    }

    /** Returns a playable result, or a failure carrying the reason to report to the user. */
    private fun tryCandidate(
        host: String,
        referer: String,
        headers: Map<String, String>,
        candidate: Candidate,
        anime: Anime,
        episode: Episode,
        language: String
    ): AnimeStreamResolver.Output? {
        Log.i(TAG, "anikoto: trying ${candidate.path} (${candidate.name}, score ${candidate.score})")
        val watchPage = Http.get(client, "$host${candidate.path}", mapOf("Referer" to referer))
            ?: return fail("title page did not load")
        val animeId = Regex("data-anime-id=\"(\\d+)\"").find(watchPage)?.groupValues?.getOrNull(1)
            ?: Regex("data-id=\"(\\d+)\"").find(watchPage)?.groupValues?.getOrNull(1)
            ?: return fail("anime id missing")

        val epBody = Http.get(client, "$host/ajax/episode/list/$animeId", headers)
            ?: return fail("episode list unreachable")
        val epHtml = Http.unescapeJson(epBody)

        // The episode list exposes the show's MAL id: a cheap, exact way to reject a name-alike.
        if (anime.malId.isNotBlank()) {
            val siteMal = Regex("data-mal=\"(\\d+)\"").find(epHtml)?.groupValues?.getOrNull(1)
            if (siteMal != null && siteMal != anime.malId) {
                Log.i(TAG, "${candidate.path} is MAL $siteMal, wanted ${anime.malId} - skipping")
                return fail("wrong show (MAL $siteMal)")
            }
        }

        val episodeToken = findEpisodeToken(epHtml, episode.number)
            ?: return fail("episode ${episode.number} missing")

        val serverDiscovery = findServers(host, episodeToken, language, headers)
        if (serverDiscovery.servers.isEmpty()) {
            return if (serverDiscovery.dubUnavailable) {
                AnimeStreamResolver.Output(
                    sources = emptyList(),
                    error = "English Dub is not available for Episode ${episode.number} on this server (Sub is available)",
                    availableAudioLanguages = serverDiscovery.availableLanguages,
                    dubUnavailable = true
                )
            } else {
                AnimeStreamResolver.Output(
                    sources = emptyList(),
                    error = "no servers offered",
                    availableAudioLanguages = serverDiscovery.availableLanguages
                )
            }
        }
        val servers = serverDiscovery.servers

        Log.i(TAG, "$host ${candidate.path} ep ${episode.number} -> ${servers.size} server(s): ${servers.joinToString { it.name }}")

        var lastError: String? = null
        for (server in servers.take(4)) {
            val embed = resolveServerUrl(host, server.linkId, headers) ?: continue

            val extracted = when {
                isMegaPlay(embed) -> megaPlay.extract(embed, referer, id, server.name, language)
                isMegaCloud(embed) -> {
                    val result = megaCloud.extract(embed, id, server.name, language)
                    AnimeStreamResolver.Output(result.sources, result.subtitles, result.error)
                }
                isStreamTape(embed) -> streamTape.extract(embed, server.name, language)
                else -> directSource(embed, server, language, headers)
                    ?.let { AnimeStreamResolver.Output(listOf(it)) }
                    ?: AnimeStreamResolver.Output(emptyList(), error = "unsupported server")
            }

            if (extracted.sources.isNotEmpty()) {
                return AnimeStreamResolver.Output(
                    sources = extracted.sources.distinctBy { it.key },
                    subtitles = extracted.subtitles.distinctBy { it.url },
                    availableAudioLanguages = serverDiscovery.availableLanguages
                )
            }
            lastError = extracted.error ?: "no playable stream"
        }
        return AnimeStreamResolver.Output(
            sources = emptyList(),
            error = lastError ?: "no playable stream",
            availableAudioLanguages = serverDiscovery.availableLanguages
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------------------------------

    private data class Candidate(val path: String, val name: String, val score: Int) {
        /** Kept so callers can report which hit they settled on. */
        override fun toString(): String = "$name ($score)"
    }

    /**
     * Ranks the site's search hits against the AniList title. Exact matches win, then prefix
     * matches, then containment; the release year breaks ties.
     */
    private fun searchCandidates(
        host: String,
        anime: Anime,
        headers: Map<String, String>
    ): List<Candidate> {
        val keywords = listOf(anime.title, anime.nativeTitle)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val hits = linkedMapOf<String, Candidate>()
        for (keyword in keywords) {
            val encoded = urlEncode(keyword)
            val url = "$host/ajax/anime/search?keyword=$encoded"
            val body = fetch(url, headers)
            if (body == null) {
                Log.w(TAG, "anikoto: search request failed for '$keyword'")
                continue
            }
            val html = Http.unescapeJson(body)

            val itemRegex = Regex(
                "<a class=\"item\" href=\"([^\"]+)\"([\\s\\S]*?)</a>",
                RegexOption.IGNORE_CASE
            )
            for (match in itemRegex.findAll(html)) {
                val path = pathOf(match.groupValues[1]) ?: continue
                val block = match.groupValues[2]
                val nameTag = Regex(
                    "<div class=\"name d-title\"(?: data-jp=\"([^\"]*)\")?>([^<]*)</div>",
                    RegexOption.IGNORE_CASE
                ).find(block) ?: continue
                val jpName = decodeEntities(nameTag.groupValues[1])
                val name = decodeEntities(nameTag.groupValues[2]).trim()
                val year = Regex("<span class=\"dot\">(\\d{4})</span>").find(block)
                    ?.groupValues?.getOrNull(1)?.toIntOrNull()
                val score = scoreOf(anime, keyword, name, jpName, year)
                val existing = hits[path]
                if (existing == null || existing.score < score) {
                    hits[path] = Candidate(path = path, name = name, score = score)
                }
            }
            Log.i(TAG, "anikoto: search '$keyword' -> ${hits.size} hit(s)")
            // An exact hit on the first keyword is as good as it gets; no need to widen the search.
            if (hits.values.any { it.score >= EXACT_SCORE }) break
        }

        val ranked = hits.values.sortedByDescending { it.score }
        // AniList titles and site titles often disagree slightly ("2nd - 3rd STAGE" vs "2nd
        // Stage"), so when nothing scores, trust the site's own relevance order instead of giving
        // up. The MAL id check in tryCandidate still rejects a wrong show.
        return if (ranked.firstOrNull()?.score ?: 0 > 0) ranked else ranked.take(MAX_CANDIDATES)
    }

    /** Like [Http.get] but reports why it failed, so on-device diagnosis is possible. */
    private fun fetch(url: String, headers: Map<String, String>): String? {
        return try {
            val builder = Request.Builder().url(url).header("User-Agent", Http.DEFAULT_UA)
            headers.forEach { (k, v) -> if (v.isNotBlank()) builder.header(k, v) }
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "anikoto: HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "anikoto: ${t.javaClass.simpleName} for $url (${t.message})")
            null
        }
    }

    /**
     * `URLEncoder` writes a space as `+`, which these endpoints reject; `%20` is required.
     * Safe for the base64 tokens too, whose literal `+` is already written as `%2B`.
     */
    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun pathOf(href: String): String? {
        href.toHttpUrlOrNull()?.let { return it.encodedPath }
        return if (href.startsWith("/")) href else null
    }

    /**
     * Scores a hit by how much of the AniList title it covers. Token overlap (rather than prefix
     * matching) is what makes near-miss names such as "2nd Stage" vs "2nd - 3rd STAGE" rank.
     */
    private fun scoreOf(
        anime: Anime,
        keyword: String,
        name: String,
        jpName: String,
        year: Int?
    ): Int {
        val queryTokens = tokensOf(keyword)
        if (queryTokens.isEmpty()) return 0
        val queryNorm = normalize(keyword)

        var best = 0
        for (candidateName in listOf(name, jpName)) {
            if (candidateName.isBlank()) continue
            val candidateTokens = tokensOf(candidateName)
            if (candidateTokens.isEmpty()) continue
            val shared = queryTokens.count { it in candidateTokens }
            if (shared == 0) continue

            var score = (500.0 * shared / queryTokens.size).toInt()
            if (normalize(candidateName) == queryNorm) score += EXACT_SCORE
            best = maxOf(best, score)
        }
        if (best == 0) return 0
        // Same title, different year usually means a movie, a spin-off or a remake.
        if (anime.year > 0 && year == anime.year) best += 25
        return best
    }

    private fun tokensOf(value: String): Set<String> = decodeEntities(value)
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 2 }
        .toSet()

    private fun normalize(value: String): String =
        decodeEntities(value).lowercase().replace(Regex("[^a-z0-9]"), "")

    /** Site titles arrive HTML-escaped, e.g. `JoJo&#039;s Bizarre Adventure`. */
    private fun decodeEntities(value: String): String = value
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("&#x27;", RegexOption.IGNORE_CASE), "'")

    // ---------------------------------------------------------------------------------------------
    // Protocol steps
    // ---------------------------------------------------------------------------------------------

    private fun ajaxHeaders(referer: String) = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to referer
    )

    private fun findEpisodeToken(epHtml: String, episodeNumber: Int): String? {
        for (anchor in Regex("<a\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(epHtml)) {
            val tag = anchor.value
            val number = Regex("data-num=\"([0-9.]+)\"").find(tag)?.groupValues?.getOrNull(1) ?: continue
            if (number.toFloatOrNull()?.toInt() != episodeNumber) continue
            Regex("data-ids=\"([^\"]+)\"").find(tag)?.groupValues?.getOrNull(1)?.let { return it }
            // Older layout: the episode id rode along in the href instead of a data-ids token.
            Regex("href=\"[^\"]*[?&]ep=(\\d+)").find(tag)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    private data class Server(val name: String, val linkId: String, val isDub: Boolean)

    private data class ServerDiscovery(
        val servers: List<Server>,
        val availableLanguages: Set<String>,
        val dubUnavailable: Boolean
    )

    internal fun parseServersHtml(html: String, language: String): Pair<List<Pair<String, String>>, Boolean> {
        val discovery = parseServersFromHtml(html, language)
        return discovery.servers.map { it.name to it.linkId } to discovery.dubUnavailable
    }

    private fun parseServersFromHtml(html: String, language: String): ServerDiscovery {
        val wantDub = language.equals("dub", true)
        val servers = mutableListOf<Server>()
        var currentIsDub = false

        // Walk the markup in order so each <li> is attributed to the sub/dub group it sits under.
        val token = Regex(
            "data-type=\"(sub|dub|raw)\"|<li\\b([^>]*)>([\\s\\S]*?)</li>",
            RegexOption.IGNORE_CASE
        )
        for (match in token.findAll(html)) {
            val type = match.groupValues[1]
            if (type.isNotBlank()) {
                currentIsDub = type.equals("dub", true)
                continue
            }
            val attrs = match.groupValues[2]
            val linkId = Regex("data-link-id=\"([^\"]+)\"").find(attrs)?.groupValues?.getOrNull(1)
                ?: Regex("data-id=\"([^\"]+)\"").find(attrs)?.groupValues?.getOrNull(1)
                ?: continue
            val itemTypeAttr = Regex("data-type=\"(sub|dub)\"", RegexOption.IGNORE_CASE).find(attrs)?.groupValues?.getOrNull(1)
            val isDubServer = if (itemTypeAttr != null) itemTypeAttr.equals("dub", true) else currentIsDub
            val rawName = match.groupValues[3].replace(Regex("<[^>]*>"), "").trim()
            val name = rawName.ifBlank { "Server" }
            servers += Server(name = name, linkId = linkId, isDub = isDubServer)
        }

        if (servers.isEmpty()) {
            return ServerDiscovery(emptyList(), emptySet(), false)
        }

        val availableLangs = buildSet {
            if (servers.any { !it.isDub }) add("sub")
            if (servers.any { it.isDub }) add("dub")
        }

        val matching = servers.filter { it.isDub == wantDub }.distinctBy { it.linkId }
        if (wantDub && matching.isEmpty()) {
            // Do NOT silently fall back to sub servers when the user explicitly requested dub!
            return ServerDiscovery(
                servers = emptyList(),
                availableLanguages = availableLangs,
                dubUnavailable = true
            )
        }

        val chosen = if (matching.isNotEmpty()) matching else servers.distinctBy { it.linkId }
        return ServerDiscovery(
            servers = chosen,
            availableLanguages = availableLangs,
            dubUnavailable = false
        )
    }

    private fun findServers(
        host: String,
        episodeToken: String,
        language: String,
        headers: Map<String, String>
    ): ServerDiscovery {
        val encoded = urlEncode(episodeToken)
        val body = Http.get(client, "$host/ajax/server/list?servers=$encoded", headers)
            ?: return ServerDiscovery(emptyList(), emptySet(), false)
        val html = Http.unescapeJson(body)
        return parseServersFromHtml(html, language)
    }

    private fun resolveServerUrl(
        host: String,
        linkId: String,
        headers: Map<String, String>
    ): String? {
        val encoded = urlEncode(linkId)
        val body = Http.get(client, "$host/ajax/server?get=$encoded", headers) ?: return null
        val json = Http.unescapeJson(body)
        Http.jsonField(json, "url")?.let { if (it.startsWith("http")) return it }
        Http.jsonField(json, "link")?.let { if (it.startsWith("http")) return it }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Embed routing
    // ---------------------------------------------------------------------------------------------

    private fun isMegaPlay(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("megaplay") || lower.contains("/stream/s-")
    }

    private fun isMegaCloud(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("megacloud") || lower.contains("/e-1/")
    }

    private fun isStreamTape(url: String): Boolean = url.lowercase().contains("streamtape")

    private fun directSource(
        url: String,
        server: Server,
        language: String,
        headers: Map<String, String>
    ): StreamSource? {
        val lower = url.lowercase()
        val isHls = lower.contains(".m3u8")
        val isProgressive = lower.contains(".mp4")
        if (!isHls && !isProgressive) return null
        return StreamSource(
            url = url,
            qualityLabel = "Auto",
            isHls = isHls,
            audioLanguage = language,
            serverId = id,
            serverName = server.name,
            headers = headers,
            isProgressive = isProgressive
        )
    }

    private fun fail(reason: String) = AnimeStreamResolver.Output(emptyList(), error = reason)

    private companion object {
        const val TAG = "ZenithResolver"
        const val EXACT_SCORE = 1000
        const val MAX_CANDIDATES = 3
    }
}
