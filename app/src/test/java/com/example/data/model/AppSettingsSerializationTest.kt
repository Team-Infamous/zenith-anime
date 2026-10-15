package com.example.data.model

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the persistence contract behind `SettingsManager.exportJson()`/`importJson()`: the whole
 * [AppSettings] object must survive a JSON round-trip through the same Moshi setup, so backups,
 * restores and the every-launch reload never silently drop a preference.
 */
class AppSettingsSerializationTest {

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(AppSettings::class.java)

    private fun roundTrip(settings: AppSettings): AppSettings =
        adapter.fromJson(adapter.toJson(settings))!!

    @Test
    fun defaultsSurviveAFullRoundTrip() {
        assertEquals(AppSettings.DEFAULT, roundTrip(AppSettings.DEFAULT))
        assertEquals(AppSettings(), roundTrip(AppSettings()))
    }

    @Test
    fun nonDefaultValuesInEveryGroupSurviveTheRoundTrip() {
        val custom = AppSettings.DEFAULT.copy(
            // player lock
            lockButtonEnabled = false,
            autoLockEnabled = true,
            autoLockDelaySec = 120,
            lockBlocksTouches = false,
            lockHapticFeedback = false,
            orientationLockMode = "Portrait",
            showLockedBadge = false,
            // downloads
            downloadQuality = "720p",
            downloadLocation = "Custom folder",
            downloadCustomFolderUri = "content://com.android.providers.downloads.documents/tree/primary%3AMovies",
            wifiOnlyDownloads = true,
            downloadConcurrency = 4,
            downloadSubtitles = "None",
            // library & series
            continueWatchingOneCard = false,
            continueWatchingLimit = 50,
            continueWatchingShowNextEpisode = false,
            friendlySeasonLabels = true,
            seasonLabelStyle = "Part",
            hideAdaptationLabel = false,
            markWatchedThresholdPercent = 75,
            // playback, gestures and hold-to-speed
            holdToSpeedMultiplier = 3.0f,
            holdToSpeedHaptic = false,
            autoNextCountdownSec = 12,
            seekDurationSec = 25,
            defaultPlaybackSpeed = 1.5f,
            // subtitles and profile
            subtitleFontSizeSp = 22,
            subtitleDelayMs = -750,
            avatarPath = "/data/user/0/app/files/avatar.png",
            themeAccent = "Crimson"
        )
        assertEquals(custom, roundTrip(custom))
    }

    @Test
    fun serializedJsonContainsEveryKnownSettingKey() {
        // Guards against a field being dropped from the model or the adapter.
        val json = adapter.toJson(AppSettings.DEFAULT)
        listOf(
            "defaultServer",
            "playbackQuality",
            "holdToSpeedMultiplier",
            "holdToSpeedHaptic",
            "lockButtonEnabled",
            "autoLockDelaySec",
            "orientationLockMode",
            "subtitleFontSizeSp",
            "downloadQuality",
            "downloadCustomFolderUri",
            "downloadConcurrency",
            "continueWatchingOneCard",
            "continueWatchingLimit",
            "seasonLabelStyle",
            "markWatchedThresholdPercent",
            "themeAccent",
            "amoledMode",
            "malUsername"
        ).forEach { key ->
            assertTrue("missing $key in $json", json.contains("\"$key\""))
        }
    }

    @Test
    fun layoutPlaygroundAndCustomizationSettingsSurviveRoundTrip() {
        val customized = AppSettings.DEFAULT.copy(
            useLegacyPlayerLayout = true,
            lockButtonSide = "Right",
            playerTopBarOrder = "lock,speed,server,audio,subtitle,pip,aspect,sleep",
            playerHiddenControls = "speed,pip",
            playerTransportPosition = "Bottom",
            playerControlSize = "Large",
            playerAccentOverride = "Sakura",
            playerOverlayDimAlpha = 0.70f,
            playerSeekbarStyle = "Thick",
            avatarUrl = "https://example.com/avatar.gif",
            avatarSvgContent = "<svg></svg>",
            reduceAnimations = true
        )
        val roundTripped = roundTrip(customized)
        assertEquals(customized, roundTripped)
        assertEquals(true, roundTripped.useLegacyPlayerLayout)
        assertEquals("Right", roundTripped.lockButtonSide)
        assertEquals("speed,pip", roundTripped.playerHiddenControls)
        assertEquals("Sakura", roundTripped.playerAccentOverride)
        assertEquals("https://example.com/avatar.gif", roundTripped.avatarUrl)
    }

    @Test
    fun partialJsonKeepsDefaultsForMissingKeysSoOlderBackupsStillLoad() {
        val parsed = adapter.fromJson("{\"defaultServer\":\"embed\",\"amoledMode\":true}")!!
        assertEquals("embed", parsed.defaultServer)
        assertTrue(parsed.amoledMode)
        // Everything else falls back to the model default instead of throwing.
        assertEquals(AppSettings.DEFAULT.playbackQuality, parsed.playbackQuality)
        assertEquals(AppSettings.DEFAULT.friendlySeasonLabels, parsed.friendlySeasonLabels)
        assertEquals(AppSettings.DEFAULT.lockButtonEnabled, parsed.lockButtonEnabled)
        assertEquals(AppSettings.DEFAULT.downloadConcurrency, parsed.downloadConcurrency)
        assertEquals(AppSettings.DEFAULT, parsed.copy(defaultServer = "mega", amoledMode = false))
    }
}
