package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for every user preference.
 *
 * The whole [AppSettings] object is persisted as one JSON blob, so anything added to the model is
 * automatically saved, restored and included in backups. Legacy single-key preferences from older
 * builds are migrated on first launch.
 */
@Suppress("TooManyFunctions")
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zenith_anime_prefs", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(AppSettings::class.java)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val raw = prefs.getString(KEY_JSON, null)
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = adapter.fromJson(raw)
                if (parsed != null) return parsed
            } catch (_: Throwable) {
                // Corrupt payload: fall through to legacy migration / defaults.
            }
        }
        return migrateLegacy()
    }

    /** Reads the old per-key preferences once, then writes them out as JSON. */
    private fun migrateLegacy(): AppSettings {
        val migrated = AppSettings(
            defaultServer = prefs.getString("default_server", AppSettings().defaultServer) ?: AppSettings().defaultServer,
            defaultLanguage = prefs.getString("default_language", "sub") ?: "sub",
            autoplay = prefs.getBoolean("autoplay", true),
            autoSkipIntro = prefs.getBoolean("auto_skip_intro", false),
            autoNextEpisode = prefs.getBoolean("auto_next_episode", true),
            saveProgressIntervalSec = prefs.getInt("save_progress_interval", 5),
            username = prefs.getString("username", "ZenithWatcher") ?: "ZenithWatcher",
            avatarPath = prefs.getString("avatar_path", null),
            themeAccent = prefs.getString("theme_accent", "Emerald") ?: "Emerald",
            amoledMode = prefs.getBoolean("amoled_mode", false),
            incognitoMode = prefs.getBoolean("incognito_mode", false),
            seekDurationSec = prefs.getInt("seek_duration", 10),
            defaultPlaybackSpeed = prefs.getFloat("default_playback_speed", 1.0f),
            videoFitMode = prefs.getString("video_fit_mode", "Fit") ?: "Fit",
            adBlockEnabled = prefs.getBoolean("ad_block_enabled", true),
            airingNotifications = prefs.getBoolean("airing_notifications", true),
            wifiOnlyDownloads = prefs.getBoolean("wifi_only_downloads", false),
            showSkipButtons = prefs.getBoolean("show_skip_buttons", true),
            holdToSpeedMultiplier = prefs.getFloat("hold_to_speed_multiplier", 2.0f),
            holdToSpeedHaptic = prefs.getBoolean("hold_to_speed_haptic", true),
            playbackQuality = prefs.getString("playback_quality", "Auto") ?: "Auto",
            downloadQuality = prefs.getString("download_quality", "1080p") ?: "1080p",
            downloadLocation = prefs.getString("download_location", "In-App") ?: "In-App",
            downloadSubtitles = prefs.getString("download_subtitles", "English") ?: "English",
            reduceAnimations = prefs.getBoolean("reduce_animations", false),
            malUsername = prefs.getString("mal_username", "") ?: "",
            anilistUsername = prefs.getString("anilist_username", "") ?: ""
        )
        persist(migrated)
        return migrated
    }

    private fun persist(value: AppSettings) {
        prefs.edit().putString(KEY_JSON, adapter.toJson(value)).apply()
    }

    fun getSettings(): AppSettings = _settings.value

    /** The one write path: mutate a copy, persist it, publish it. */
    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        persist(next)
        _settings.value = next
    }

    fun setAll(value: AppSettings) {
        persist(value)
        _settings.value = value
    }

    fun resetSettings() {
        prefs.edit().clear().apply()
        _settings.value = AppSettings()
        persist(_settings.value)
    }

    /** Resets only player/playback preferences, leaving library, downloads and profile intact. */
    fun resetPlayerPreferences() {
        update { current ->
            current.copy(
                defaultServer = AppSettings().defaultServer,
                defaultLanguage = AppSettings().defaultLanguage,
                playbackQuality = AppSettings().playbackQuality,
                defaultPlaybackSpeed = AppSettings().defaultPlaybackSpeed,
                autoplay = AppSettings().autoplay,
                autoNextEpisode = AppSettings().autoNextEpisode,
                autoSkipIntro = AppSettings().autoSkipIntro,
                autoSkipOutro = AppSettings().autoSkipOutro,
                holdToSpeedEnabled = AppSettings().holdToSpeedEnabled,
                holdToSpeedMultiplier = AppSettings().holdToSpeedMultiplier,
                holdToSpeedHaptic = AppSettings().holdToSpeedHaptic,
                doubleTapSeekEnabled = AppSettings().doubleTapSeekEnabled,
                seekDurationSec = AppSettings().seekDurationSec,
                orientationLockMode = AppSettings().orientationLockMode,
                subtitlesEnabled = AppSettings().subtitlesEnabled,
                subtitleFontSizeSp = AppSettings().subtitleFontSizeSp,
                subtitleTextColor = AppSettings().subtitleTextColor,
                subtitleBackgroundColor = AppSettings().subtitleBackgroundColor,
                lockButtonSide = AppSettings().lockButtonSide,
                useLegacyPlayerLayout = AppSettings().useLegacyPlayerLayout,
                playerTopBarOrder = AppSettings().playerTopBarOrder,
                playerHiddenControls = AppSettings().playerHiddenControls,
                playerTransportPosition = AppSettings().playerTransportPosition,
                playerControlSize = AppSettings().playerControlSize,
                playerAccentOverride = AppSettings().playerAccentOverride,
                playerOverlayDimAlpha = AppSettings().playerOverlayDimAlpha,
                playerSeekbarStyle = AppSettings().playerSeekbarStyle
            )
        }
    }

    /** Restores only the player layout playground customizations to defaults. */
    fun resetPlayerLayoutPlayground() {
        update { current ->
            current.copy(
                playerTopBarOrder = AppSettings().playerTopBarOrder,
                playerHiddenControls = AppSettings().playerHiddenControls,
                playerTransportPosition = AppSettings().playerTransportPosition,
                playerControlSize = AppSettings().playerControlSize,
                playerAccentOverride = AppSettings().playerAccentOverride,
                playerOverlayDimAlpha = AppSettings().playerOverlayDimAlpha,
                playerSeekbarStyle = AppSettings().playerSeekbarStyle,
                lockButtonSide = AppSettings().lockButtonSide,
                useLegacyPlayerLayout = false
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Typed conveniences. Kept for existing call sites and for readability in new UI code.
    // ---------------------------------------------------------------------------------------------

    fun updateThemeAccent(accent: String) = update { it.copy(themeAccent = accent) }
    fun updateDefaultServer(server: String) = update { it.copy(defaultServer = server) }
    fun updateDefaultLanguage(language: String) = update { it.copy(defaultLanguage = language) }
    fun updateAutoplay(enabled: Boolean) = update { it.copy(autoplay = enabled) }
    fun updateAutoSkipIntro(enabled: Boolean) = update { it.copy(autoSkipIntro = enabled) }
    fun updateAutoSkipOutro(enabled: Boolean) = update { it.copy(autoSkipOutro = enabled) }
    fun updateAutoNext(enabled: Boolean) = update { it.copy(autoNextEpisode = enabled) }
    fun updateSeekInterval(sec: Int) = update { it.copy(seekDurationSec = sec) }
    fun updateDefaultSpeed(speed: Float) = update { it.copy(defaultPlaybackSpeed = speed) }
    fun updateAmoledMode(enabled: Boolean) = update { it.copy(amoledMode = enabled) }
    fun updateIncognitoMode(enabled: Boolean) = update { it.copy(incognitoMode = enabled) }
    fun updateAiringNotifications(enabled: Boolean) = update { it.copy(airingNotifications = enabled) }
    fun updateWifiOnlyDownloads(enabled: Boolean) = update { it.copy(wifiOnlyDownloads = enabled) }
    fun updateShowSkipButtons(enabled: Boolean) = update { it.copy(showSkipButtons = enabled) }
    fun updatePlaybackQuality(quality: String) = update { it.copy(playbackQuality = quality) }
    fun updateReduceAnimations(enabled: Boolean) = update { it.copy(reduceAnimations = enabled) }
    fun updateMalUsername(username: String) = update { it.copy(malUsername = username) }
    fun updateAnilistUsername(username: String) = update { it.copy(anilistUsername = username) }

    fun updateProfile(name: String, bio: String? = null) = update {
        if (bio != null) it.copy(username = name, profileBio = bio) else it.copy(username = name)
    }

    fun updateAvatar(path: String?, url: String = "") = update {
        it.copy(avatarPath = path, avatarUrl = url)
    }

    fun updateAvatarPath(path: String?) = update { it.copy(avatarPath = path) }

    fun updateHoldToSpeed(multiplier: Float, haptic: Boolean) =
        update { it.copy(holdToSpeedMultiplier = multiplier, holdToSpeedHaptic = haptic) }

    fun updateDownloadSettings(quality: String, location: String, subtitles: String) = update {
        it.copy(downloadQuality = quality, downloadLocation = location, downloadSubtitles = subtitles)
    }

    fun updateLockSettings(
        lockButtonEnabled: Boolean,
        autoLockEnabled: Boolean,
        autoLockDelaySec: Int,
        blockTouches: Boolean,
        orientationLockMode: String
    ) = update {
        it.copy(
            lockButtonEnabled = lockButtonEnabled,
            autoLockEnabled = autoLockEnabled,
            autoLockDelaySec = autoLockDelaySec,
            lockBlocksTouches = blockTouches,
            orientationLockMode = orientationLockMode
        )
    }

    fun updateSubtitleSettings(
        enabled: Boolean,
        language: String,
        fontSizeSp: Int,
        textColor: String,
        backgroundColor: String,
        outline: Boolean,
        bold: Boolean,
        position: String,
        delayMs: Int
    ) = update {
        it.copy(
            subtitlesEnabled = enabled,
            subtitleLanguage = language,
            subtitleFontSizeSp = fontSizeSp,
            subtitleTextColor = textColor,
            subtitleBackgroundColor = backgroundColor,
            subtitleOutline = outline,
            subtitleBold = bold,
            subtitlePosition = position,
            subtitleDelayMs = delayMs
        )
    }

    fun updateLibrarySettings(
        continueWatchingOneCard: Boolean,
        continueWatchingLimit: Int,
        friendlySeasonLabels: Boolean,
        seasonLabelStyle: String
    ) = update {
        it.copy(
            continueWatchingOneCard = continueWatchingOneCard,
            continueWatchingLimit = continueWatchingLimit,
            friendlySeasonLabels = friendlySeasonLabels,
            seasonLabelStyle = seasonLabelStyle
        )
    }

    /** Serializes the complete preference set for backup export. */
    fun exportJson(): String = adapter.toJson(_settings.value)

    /** Restores a complete preference set from a backup document. */
    fun importJson(json: String): Boolean {
        return try {
            val parsed = adapter.fromJson(json) ?: return false
            setAll(parsed)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private companion object {
        const val KEY_JSON = "settings_json_v3"
    }
}
