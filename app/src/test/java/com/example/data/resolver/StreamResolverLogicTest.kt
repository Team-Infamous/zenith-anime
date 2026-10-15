package com.example.data.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the resolver plumbing that has no site dependency: the download quality picker, the
 * consumer-facing failure text and [StreamSource] identity.
 */
class StreamResolverLogicTest {

    private fun hls(quality: String, serverId: String = "zoro") = StreamSource(
        url = "https://cdn.example.com/$quality/index.m3u8",
        qualityLabel = quality,
        isHls = true,
        audioLanguage = "sub",
        serverId = serverId,
        serverName = "HD-1"
    )

    private fun progressive(quality: String) = StreamSource(
        url = "https://cdn.example.com/$quality.mp4",
        qualityLabel = quality,
        isHls = false,
        audioLanguage = "sub",
        serverId = "zoro",
        serverName = "HD-1",
        isProgressive = true
    )

    // ------------------------------------------------------- pickForDownload

    @Test
    fun pickForDownloadPrefersTheExactRequestedQuality() {
        val sources = listOf(hls("1080p"), hls("720p"), hls("480p"))
        assertEquals("720p", StreamResolverRegistry.pickForDownload(sources, "720p")?.qualityLabel)
        assertEquals("1080p", StreamResolverRegistry.pickForDownload(sources, "1080p")?.qualityLabel)
        // Quality labels come from settings and may carry extra wording.
        assertEquals("1080p", StreamResolverRegistry.pickForDownload(sources, "Best 1080p")?.qualityLabel)
    }

    @Test
    fun pickForDownloadFallsBackToTheProgressiveSource() {
        val sources = listOf(hls("1080p"), hls("720p"), progressive("480p"))
        // 360p is not offered, so the single downloadable file wins.
        assertEquals("480p", StreamResolverRegistry.pickForDownload(sources, "360p")?.qualityLabel)
        // "Auto" has no numeric target, so the progressive file wins.
        assertEquals("480p", StreamResolverRegistry.pickForDownload(sources, "Auto")?.qualityLabel)
    }

    @Test
    fun pickForDownloadFallsBackToTheFirstSourceAndHandlesEmptyInput() {
        val sources = listOf(hls("1080p"), hls("720p"))
        assertEquals("1080p", StreamResolverRegistry.pickForDownload(sources, "480p")?.qualityLabel)
        assertEquals("1080p", StreamResolverRegistry.pickForDownload(sources, "Auto")?.qualityLabel)
        assertNull(StreamResolverRegistry.pickForDownload(emptyList(), "1080p"))
    }

    // ------------------------------------------------------- failureSummary

    @Test
    fun failureSummaryExplainsWhatWentWrongWithoutLeakingNulls() {
        val noServers = ResolveResult(attempts = emptyList(), queryTitle = "Some Anime")
        assertTrue(noServers.failureSummary().isNotBlank())

        val result = ResolveResult(
            attempts = listOf(
                ResolveAttempt("zoro", "Zoro (HiAnime)", emptyList(), error = "No host reachable"),
                ResolveAttempt("aniwatch", "AniWatch", emptyList(), error = null)
            ),
            queryTitle = "Some Anime"
        )

        val summary = result.failureSummary()
        assertTrue(summary.contains("Zoro (HiAnime)"))
        assertTrue(summary.contains("No host reachable"))
        assertTrue(summary.contains("AniWatch"))
        // A resolver that reported nothing still gets readable copy, not "null".
        assertTrue(summary.contains("No stream returned"))
        assertFalse(summary.contains("null", ignoreCase = true))
        assertFalse(noServers.failureSummary().contains("null", ignoreCase = true))
    }

    @Test
    fun failureSummaryIsInertWhenStreamsWereFound() {
        val result = ResolveResult(
            attempts = listOf(ResolveAttempt("zoro", "Zoro (HiAnime)", listOf(hls("1080p")))),
            queryTitle = "Some Anime"
        )
        assertEquals("No playable stream found.", result.failureSummary())
        assertTrue(result.hasAnySource)
        assertEquals(1, result.sources.size)
    }

    @Test
    fun failureSummaryOnlyCountsAttemptsThatReturnedNothing() {
        val result = ResolveResult(
            attempts = listOf(
                ResolveAttempt("zoro", "Zoro (HiAnime)", emptyList(), error = "blocked"),
                ResolveAttempt("allanime", "AllAnime", listOf(hls("1080p")))
            ),
            queryTitle = "Some Anime"
        )
        assertEquals("No playable stream found.", result.failureSummary())
        assertEquals(1, result.sources.size)
    }

    // ------------------------------------------------------- StreamSource.key

    @Test
    fun streamSourceKeyIsStableAndDistinguishesServerQualityAndUrl() {
        val base = hls("1080p")
        assertEquals("zoro|1080p|https://cdn.example.com/1080p/index.m3u8", base.key)
        // Same inputs produce the same identity, so de-duplication is deterministic.
        assertEquals(base.key, hls("1080p").key)
        assertNotEquals(base.key, hls("720p").key)
        assertNotEquals(base.key, hls("1080p", serverId = "aniwatch").key)
        assertNotEquals(
            base.key,
            base.copy(url = "https://other.example.com/1080p/index.m3u8").key
        )
    }
}
