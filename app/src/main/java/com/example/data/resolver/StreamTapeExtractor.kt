package com.example.data.resolver

import android.util.Log
import okhttp3.OkHttpClient

/**
 * Port of `aniyomi.lib.streamtapeextractor.StreamTapeExtractor` recovered from the installed
 * Zoro/HiAnime extension.
 *
 * Flow: `/e/<id>` page -> the `robotlink` script tag -> rebuild the direct link by concatenating
 * the literal fragment with the `+ ('xcd` fragment -> a progressive mp4 served by `get_video`.
 *
 * The upstream extractor used JSoup (`selectFirst("script:containsData(...)")`); JSoup is not a
 * dependency of this app, so the same script block is located with a regex over the raw HTML.
 */
class StreamTapeExtractor(
    private val client: OkHttpClient,
    /**
     * Tag stored on every [StreamSource] this extractor returns. `AnikotoResolver` passes its own
     * resolver id so StreamTape sources group with the rest of that server's streams.
     */
    private val serverId: String = DEFAULT_SERVER_ID
) {

    /**
     * Blocking, like [MegaCloudExtractor.extract]: callers already run inside
     * `withContext(Dispatchers.IO)`.
     */
    fun extract(
        url: String,
        serverName: String,
        language: String
    ): AnimeStreamResolver.Output {
        return try {
            val pageUrl = embedPageUrl(url)
                ?: return fail("Could not read the StreamTape file id")

            val html = Http.get(client, pageUrl, pageHeaders)
                ?: return fail("StreamTape page did not load")

            val script = robotLinkScript(html)
                ?: return fail("StreamTape page layout changed")

            if (!script.contains(ROBOTLINK_PREFIX) || !script.contains(TOKEN_PREFIX)) {
                return fail("StreamTape page layout changed")
            }

            // Mirrors upstream exactly: everything before the first quote is the main fragment,
            // then the second half is rebuilt from the `+ ('xcd` declaration.
            val remainder = script.substringAfter(ROBOTLINK_PREFIX)
            val link = "https:" +
                remainder.substringBefore("'") +
                remainder.substringAfter(TOKEN_PREFIX).substringBefore("'")

            if (!link.startsWith("http") || !link.contains("get_video")) {
                return fail("Could not read the direct link")
            }

            AnimeStreamResolver.Output(
                sources = listOf(
                    StreamSource(
                        url = link,
                        qualityLabel = "Auto",
                        isHls = false,
                        audioLanguage = language,
                        serverId = serverId,
                        serverName = serverName,
                        headers = pageHeaders,
                        isProgressive = true
                    )
                )
            )
        } catch (t: Throwable) {
            Log.w(TAG, "extract failed: ${t.javaClass.simpleName}: ${t.message}")
            fail(t.javaClass.simpleName)
        }
    }

    /** Upstream normalises anything that is not already an `/e/` link by taking the 5th path part. */
    private fun embedPageUrl(url: String): String? {
        if (url.startsWith(EMBED_PREFIX)) return url
        val id = url.split("/").getOrNull(4) ?: return null
        if (id.isBlank()) return null
        return EMBED_PREFIX + id
    }

    /** Equivalent of `selectFirst("script:containsData(document.getElementById('robotlink'))").data()`. */
    private fun robotLinkScript(html: String): String? {
        val blocks = Regex("<script\\b[^>]*>([\\s\\S]*?)</script>", RegexOption.IGNORE_CASE)
            .findAll(html)
        return blocks.map { it.groupValues[1] }
            .firstOrNull { it.contains(MARKER) }
    }

    private fun fail(reason: String) = AnimeStreamResolver.Output(emptyList(), error = reason)

    private val pageHeaders = mapOf("Referer" to "$ORIGIN/")

    companion object {
        private const val ORIGIN = "https://streamtape.com"
        private const val EMBED_PREFIX = "$ORIGIN/e/"
        private const val MARKER = "document.getElementById('robotlink')"
        private const val ROBOTLINK_PREFIX =
            "document.getElementById('robotlink').innerHTML = '"
        private const val TOKEN_PREFIX = "+ ('xcd"
        private const val DEFAULT_SERVER_ID = "streamtape"
        private const val TAG = "ZenithResolver"
    }
}
