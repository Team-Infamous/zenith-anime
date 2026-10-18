package com.example.data.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [HlsParser]: master vs media playlist detection, URI resolution and the quality picker
 * that both the player source menu and the download quality setting depend on.
 */
class HlsParserTest {

    private val masterUrl = "https://cdn.example.com/hls/master.m3u8"

    private val mediaUrl = "https://cdn.example.com/hls/720/index.m3u8"

    private val master = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
        1080/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
        720/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480,CODECS="avc1.64001e,mp4a.40.2"
        480/index.m3u8
    """.trimIndent()

    private val media = """
        #EXTM3U
        #EXT-X-VERSION:3
        #EXT-X-TARGETDURATION:10
        #EXT-X-MEDIA-SEQUENCE:0
        #EXTINF:10.0,
        seg-0.ts
        #EXTINF:10.0,
        seg-1.ts
        #EXTINF:10.0,
        seg-2.ts
        #EXT-X-ENDLIST
    """.trimIndent()

    // ------------------------------------------------------------------------ isMaster

    @Test
    fun masterPlaylistIsDetectedByStreamInfTag() {
        assertTrue(HlsParser.isMaster(master))
        assertFalse(HlsParser.isMaster(media))
    }

    // ------------------------------------------------------------------------ resolveUri

    @Test
    fun resolveUriLeavesAbsoluteUrisUntouched() {
        assertEquals(
            "https://other.example.com/x.m3u8",
            HlsParser.resolveUri(masterUrl, "https://other.example.com/x.m3u8")
        )
        assertEquals(
            "http://cdn.example.com/live/y.m3u8",
            HlsParser.resolveUri(masterUrl, "http://cdn.example.com/live/y.m3u8")
        )
    }

    @Test
    fun resolveUriResolvesRootRelativeAndRelativeUris() {
        assertEquals(
            "https://cdn.example.com/root/playlist.m3u8",
            HlsParser.resolveUri(masterUrl, "/root/playlist.m3u8")
        )
        assertEquals(
            "https://cdn.example.com/hls/720/index.m3u8",
            HlsParser.resolveUri(masterUrl, "720/index.m3u8")
        )
        assertEquals(
            "https://cdn.example.com/hls/seg-0.ts",
            HlsParser.resolveUri(masterUrl, "seg-0.ts")
        )
    }

    @Test
    fun resolveUriTrimsWhitespaceAndSurvivesUnparseableBases() {
        assertEquals(
            "https://cdn.example.com/hls/720/index.m3u8",
            HlsParser.resolveUri(masterUrl, "  720/index.m3u8  ")
        )
        assertEquals(
            "https://cdn.example.com/hls/x.m3u8",
            HlsParser.resolveUri(masterUrl, "  https://cdn.example.com/hls/x.m3u8  ")
        )
        // A non-http base cannot be resolved against, so the raw relative URI is returned.
        assertEquals("seg-0.ts", HlsParser.resolveUri("not a url", "seg-0.ts"))
    }

    // ------------------------------------------------------------------------ variants

    @Test
    fun variantsParseResolutionBandwidthCodecsAndUrl() {
        val variants = HlsParser.variants(masterUrl, master)
        assertEquals(3, variants.size)
        // Best quality first, so callers can take the head as "highest".
        assertEquals(listOf("1080p", "720p", "480p"), variants.map { it.label })
        assertEquals(listOf(1080, 720, 480), variants.map { it.height })
        assertEquals(6000000, variants.first().bandwidth)
        assertEquals("avc1.640028,mp4a.40.2", variants.first().codecs)
        assertEquals("https://cdn.example.com/hls/1080/index.m3u8", variants.first().url)
        assertEquals("https://cdn.example.com/hls/480/index.m3u8", variants.last().url)
    }

    @Test
    fun variantWithoutResolutionFallsBackToAutoLabel() {
        val variant = HlsParser.Variant(
            url = "https://cdn.example.com/hls/x.m3u8",
            resolution = "",
            bandwidth = 1000
        )
        assertEquals(0, variant.height)
        assertEquals("Auto", variant.label)
    }

    @Test
    fun variantHeightParsesWidthTimesHeightAndHeightWithPSuffix() {
        val v1 = HlsParser.Variant(url = "https://cdn.example.com/1080.m3u8", resolution = "1920x1080", bandwidth = 5000000)
        val v2 = HlsParser.Variant(url = "https://cdn.example.com/720.m3u8", resolution = "720p", bandwidth = 2500000)
        assertEquals(1080, v1.height)
        assertEquals("1080p", v1.label)
        assertEquals(720, v2.height)
        assertEquals("720p", v2.label)
    }

    @Test
    fun variantsBreakHeightTiesByBandwidthDescending() {
        val tieMaster = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=3500000,RESOLUTION=1920x1080
            1080_low/index.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=6500000,RESOLUTION=1920x1080
            1080_high/index.m3u8
        """.trimIndent()
        val variants = HlsParser.variants(masterUrl, tieMaster)
        assertEquals(2, variants.size)
        assertEquals(6500000, variants.first().bandwidth)
        assertEquals("https://cdn.example.com/hls/1080_high/index.m3u8", HlsParser.pick(variants, "1080p")?.url)
    }

    // ------------------------------------------------------------------------ pick

    @Test
    fun pickChoosesHighestQualityNotAboveThePreference() {
        val variants = HlsParser.variants(masterUrl, master)
        assertEquals("1080p", HlsParser.pick(variants, "1080p")?.label)
        assertEquals("720p", HlsParser.pick(variants, "720p")?.label)
        assertEquals("480p", HlsParser.pick(variants, "480p")?.label)
        // Nothing better than 1080 exists, so the best available is used.
        assertEquals("1080p", HlsParser.pick(variants, "2160p")?.label)
        // Nothing at or below 360 exists, so the smallest available is used.
        assertEquals("480p", HlsParser.pick(variants, "360p")?.label)
        // "Auto" carries no number: the highest quality wins.
        assertEquals("1080p", HlsParser.pick(variants, "Auto")?.label)
    }

    @Test
    fun pickReturnsNullOnlyForAnEmptyPlaylist() {
        assertNull(HlsParser.pick(emptyList(), "720p"))
        assertNull(HlsParser.pick(emptyList(), "Auto"))
    }

    // ------------------------------------------------------------------------ segments

    @Test
    fun segmentsResolveEveryNonTagLine() {
        assertEquals(
            listOf(
                "https://cdn.example.com/hls/720/seg-0.ts",
                "https://cdn.example.com/hls/720/seg-1.ts",
                "https://cdn.example.com/hls/720/seg-2.ts"
            ),
            HlsParser.segments(mediaUrl, media)
        )
    }
}
