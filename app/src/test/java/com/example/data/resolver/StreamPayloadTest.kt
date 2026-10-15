package com.example.data.resolver

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the payload handling that stream resolution depends on.
 *
 * The bug these lock down: `getSources` JSON-escapes its URLs (`https:\/\/host\/…`). Parsing that
 * without unescaping made `/subtitles/` invisible, so the media directory was derived one path
 * segment too deep and every episode died with HTTP 404 on `…/subtitles/master.m3u8`.
 */
class StreamPayloadTest {

    private val extractor = MegaPlayExtractor(OkHttpClient())

    // ------------------------------------------------------------- Http.unescapeJson

    @Test
    fun unescapeJsonTurnsEscapedUrlsIntoRealOnes() {
        val raw = """{"tracks":[{"file":"https:\/\/n6j2y.shadowharbor.website\/anime\/6974\/e99f\/subtitles\/track_0_eng.vtt","label":"English","kind":"captions","default":true}]}"""
        val clean = Http.unescapeJson(raw)

        assertTrue(
            clean.contains(
                "https://n6j2y.shadowharbor.website/anime/6974/e99f/subtitles/track_0_eng.vtt"
            )
        )
        assertFalse(clean.contains("\\/"))
    }

    @Test
    fun unescapeJsonAlsoFixesJsonQuotedHtml() {
        val escaped = """<li data-link-id=\"abc123\" data-sv-id=\"323\">HD-1</li>"""
        assertEquals("<li data-link-id=\"abc123\" data-sv-id=\"323\">HD-1</li>", Http.unescapeJson(escaped))
    }

    @Test
    fun unescapeJsonLeavesAlreadyCleanPayloadsAlone() {
        val cleanJson = """{"status":200,"result":{"url":"https://megaplay.buzz/stream/s-2/1/sub"}}"""
        assertEquals(cleanJson, Http.unescapeJson(cleanJson))
    }

    // ------------------------------------------------------------- mediaDirOf

    @Test
    fun mediaDirStripsTheSubtitlesFolder() {
        val track = "https://n6j2y.shadowharbor.website/anime/6974/e99f/subtitles/track_0_eng.vtt"
        assertEquals(
            "https://n6j2y.shadowharbor.website/anime/6974/e99f",
            extractor.mediaDirOf(track)
        )
    }

    @Test
    fun escapedPayloadStillYieldsTheRightMediaDir() {
        // Exactly the shape the endpoint returns: URLs escaped inside the JSON document.
        val raw = """{"tracks":[{"file":"https:\/\/cdn.example.com\/anime\/aa\/bb\/subtitles\/ara-3.vtt","kind":"captions"}],"server":4}"""
        val clean = Http.unescapeJson(raw)
        val trackUrl = Regex("\"file\"\\s*:\\s*\"(.*?)\"").find(clean)!!.groupValues[1]

        val dir = extractor.mediaDirOf(trackUrl)
        assertEquals("https://cdn.example.com/anime/aa/bb", dir)
        assertFalse("$dir/master.m3u8".contains("/subtitles/"))
    }

    @Test
    fun mediaDirFallsBackToTheParentFolderWithoutASubtitlesSegment() {
        val direct = "https://cdn.example.com/anime/aa/bb/master.m3u8"
        assertEquals("https://cdn.example.com/anime/aa/bb", extractor.mediaDirOf(direct))
    }

    @Test
    fun mediaDirRejectsUnusableTrackUrls() {
        assertNull(extractor.mediaDirOf("track_0_eng.vtt"))
        assertNull(extractor.mediaDirOf(""))
    }

    // ------------------------------------------------------------- playerFileId

    @Test
    fun playerFileIdReadsTheIdOffThePlayerElement() {
        // Real markup: the attributes sit on the following lines, not on the opening tag line.
        val page = """
            <div class="mg-3mb3d">
                <div class="mg3-player">
                    <div class="fix-area" id="megaplay-player"
                        data-id="178189"
                        data-realid="694507"
                        data-mediaid="8879"
                        data-fileversion="0">
        """.trimIndent()

        assertEquals("178189", extractor.playerFileId(page))
    }

    @Test
    fun playerFileIdFallsBackToAnyDataId() {
        assertEquals("12345", extractor.playerFileId("""<div data-id="12345">"""))
        assertNull(extractor.playerFileId("<html><body>no player here</body></html>"))
    }
}
