package com.example.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileCustomizationTest {

    @Test
    fun sanitizeSvgAcceptsValidSvg() {
        val safeSvg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="#00D26A" />
            </svg>
        """.trimIndent()
        assertTrue(sanitizeSvgText(safeSvg))
    }

    @Test
    fun sanitizeSvgRejectsScriptTags() {
        val maliciousSvg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <script type="text/javascript">alert('xss');</script>
                <circle cx="50" cy="50" r="40" fill="red" />
            </svg>
        """.trimIndent()
        assertFalse(sanitizeSvgText(maliciousSvg))
    }

    @Test
    fun sanitizeSvgRejectsJavascriptScheme() {
        val maliciousSvg = """
            <svg xmlns="http://www.w3.org/2000/svg">
                <a href="javascript:alert(1)"><text>Click</text></a>
            </svg>
        """.trimIndent()
        assertFalse(sanitizeSvgText(maliciousSvg))
    }

    @Test
    fun sanitizeSvgRejectsInlineEventHandlers() {
        val withOnload = """<svg onload="alert(1)" width="100" height="100"></svg>"""
        val withOnerror = """<svg><image href="x" onerror="alert(1)" /></svg>"""
        val withOnclick = """<svg><rect onclick="alert(1)" width="10" height="10"/></svg>"""

        assertFalse(sanitizeSvgText(withOnload))
        assertFalse(sanitizeSvgText(withOnerror))
        assertFalse(sanitizeSvgText(withOnclick))
    }

    @Test
    fun sanitizeSvgRejectsXxeEntityExpansion() {
        val xxeSvg = """
            <?xml version="1.0"?>
            <!DOCTYPE svg [
                <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <svg>&xxe;</svg>
        """.trimIndent()
        assertFalse(sanitizeSvgText(xxeSvg))
    }

    @Test
    fun validateAvatarUrlAcceptsValidHttpAndHttps() {
        val (validHttps, errHttps) = validateAvatarUrl("https://example.com/avatar.gif")
        assertTrue(validHttps)
        assertNull(errHttps)

        val (validHttp, errHttp) = validateAvatarUrl("http://cdn.zenith.org/sprites/avatar.svg")
        assertTrue(validHttp)
        assertNull(errHttp)
    }

    @Test
    fun validateAvatarUrlRejectsInvalidSchemes() {
        val (validJs, errJs) = validateAvatarUrl("javascript:alert(1)")
        assertFalse(validJs)
        assertNotNull(errJs)

        val (validFile, errFile) = validateAvatarUrl("file:///data/secret.png")
        assertFalse(validFile)
        assertNotNull(errFile)

        val (validContent, errContent) = validateAvatarUrl("content://providers/images/1")
        assertFalse(validContent)
        assertNotNull(errContent)
    }

    @Test
    fun validateAvatarUrlHandlesMalformedStrings() {
        val (validEmptyHost, errEmptyHost) = validateAvatarUrl("https://")
        assertFalse(validEmptyHost)
        assertNotNull(errEmptyHost)
    }

    @Test
    fun getEffectiveAvatarModelReturnsExpectedModels() {
        // Empty/null should return default fallback
        val defaultModel = getEffectiveAvatarModel(null, "")
        assertTrue(defaultModel is String && (defaultModel as String).startsWith("http"))

        // Remote URL in path
        val remoteModel = getEffectiveAvatarModel("https://example.com/avatar.png", "")
        assertEquals("https://example.com/avatar.png", remoteModel)

        // Remote URL in url param
        val urlParamModel = getEffectiveAvatarModel(null, "https://example.com/anime.gif")
        assertEquals("https://example.com/anime.gif", urlParamModel)

        // Local file path
        val expectedFile = File("/data/user/0/com.example/files/avatar.jpg")
        val localModel = getEffectiveAvatarModel("/data/user/0/com.example/files/avatar.jpg", "")
        assertTrue(localModel is File)
        assertEquals(expectedFile, localModel as File)
    }
}
