package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

/** Player and playback preferences. Every option is user-controlled and persisted instantly. */
@Composable
fun PlayerSettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    onOpenLayoutPlayground: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())
    val settingsManager = repository.settingsManager

    SettingsScaffold(
        title = "Player",
        subtitle = "Playback, gestures, lock and video output",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "STREAMING") {
            SettingChoiceRow(
                title = "Default Server",
                subtitle = "Where episodes are streamed from when you press play",
                currentValue = SERVER_OPTIONS.firstOrNull { it.second == settings.defaultServer }?.first
                    ?: settings.defaultServer,
                options = SERVER_OPTIONS.map { it.first },
                onSelect = { label ->
                    val id = SERVER_OPTIONS.firstOrNull { it.first == label }?.second ?: settings.defaultServer
                    settingsManager.update { it.copy(defaultServer = id) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Default Audio",
                subtitle = "Prefer Japanese audio with subtitles, or the English dub",
                currentValue = LANGUAGE_OPTIONS.firstOrNull { it.second == settings.defaultLanguage }?.first
                    ?: settings.defaultLanguage,
                options = LANGUAGE_OPTIONS.map { it.first },
                onSelect = { label ->
                    val id = LANGUAGE_OPTIONS.firstOrNull { it.first == label }?.second ?: settings.defaultLanguage
                    settingsManager.update { it.copy(defaultLanguage = id) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Playback Quality",
                subtitle = "Auto adapts to your connection speed",
                currentValue = settings.playbackQuality,
                options = listOf("Auto", "1080p", "720p", "480p", "360p"),
                onSelect = { settingsManager.updatePlaybackQuality(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Automatic Server Fallback",
                subtitle = "Try the next server automatically when a stream fails",
                checked = settings.autoFailover,
                onCheckedChange = { settingsManager.update { s -> s.copy(autoFailover = it) } }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Servers To Try",
                subtitle = "How many alternative servers to attempt before giving up",
                value = settings.failoverServerCount,
                range = 1f..5f,
                steps = 3,
                suffix = "",
                onChange = { settingsManager.update { s -> s.copy(failoverServerCount = it) } }
            )
        }

        SettingsSection(title = "PLAYBACK BEHAVIOUR") {
            SettingSwitchRow(
                title = "Start Playing Automatically",
                subtitle = "Begin playback as soon as the episode is ready",
                checked = settings.autoplay,
                onCheckedChange = { settingsManager.updateAutoplay(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Autoplay Next Episode",
                subtitle = "Continue to the next episode when this one finishes",
                checked = settings.autoNextEpisode,
                onCheckedChange = { settingsManager.updateAutoNext(it) }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Next Episode Countdown",
                subtitle = "Seconds to wait before the next episode starts",
                value = settings.autoNextCountdownSec,
                range = 3f..30f,
                steps = 8,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(autoNextCountdownSec = it) } }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Default Playback Speed",
                subtitle = "Speed used when an episode first starts",
                value = settings.defaultPlaybackSpeed,
                valueRange = 0.25f..3f,
                steps = 10,
                valueLabel = { String.format(java.util.Locale.US, "%.2fx", it) },
                onValueChange = { settingsManager.updateDefaultSpeed(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Auto Skip Intro",
                subtitle = "Jump past the opening theme automatically",
                checked = settings.autoSkipIntro,
                onCheckedChange = { settingsManager.updateAutoSkipIntro(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Auto Skip Outro",
                subtitle = "Jump past the ending theme automatically",
                checked = settings.autoSkipOutro,
                onCheckedChange = { settingsManager.updateAutoSkipOutro(it) }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Intro Length",
                subtitle = "How far into the episode the opening theme ends",
                value = settings.introSkipSeconds,
                range = 15f..180f,
                steps = 10,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(introSkipSeconds = it) } }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Outro Length",
                subtitle = "How long before the end the credits begin",
                value = settings.outroSkipSeconds,
                range = 15f..180f,
                steps = 10,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(outroSkipSeconds = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Show Skip Buttons",
                subtitle = "Display intro and outro skip buttons in the player",
                checked = settings.showSkipButtons,
                onCheckedChange = { settingsManager.updateShowSkipButtons(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Save Progress Every Second",
                subtitle = "Saves watch progress every single second to eliminate progress jumps",
                checked = settings.saveProgressEverySecond,
                onCheckedChange = { settingsManager.update { s -> s.copy(saveProgressEverySecond = it) } }
            )
            if (!settings.saveProgressEverySecond) {
                SettingsDivider()
                IntSliderRow(
                    title = "Save Progress Interval",
                    subtitle = "How often your watch position is stored in database",
                    value = settings.saveProgressIntervalSec,
                    range = 1f..60f,
                    steps = 11,
                    suffix = "s",
                    onChange = { settingsManager.update { s -> s.copy(saveProgressIntervalSec = it) } }
                )
            }
            SettingsDivider()
            IntSliderRow(
                title = "Resume Threshold",
                subtitle = "Ignore resume points shorter than this",
                value = settings.resumeThresholdSec,
                range = 5f..120f,
                steps = 22,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(resumeThresholdSec = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Ask Before Resuming",
                subtitle = "Offer to continue where you left off instead of jumping straight in",
                checked = settings.resumePromptEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(resumePromptEnabled = it) } }
            )
        }

        SettingsSection(title = "GESTURES") {
            SettingSwitchRow(
                title = "Double Tap To Seek",
                subtitle = "Tap the left or right side of the video to jump backwards or forwards",
                checked = settings.doubleTapSeekEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(doubleTapSeekEnabled = it) } }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Seek Step",
                subtitle = "How far a double tap or the skip buttons move",
                value = settings.seekDurationSec,
                range = 5f..60f,
                steps = 10,
                suffix = "s",
                onChange = { settingsManager.updateSeekInterval(it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Swipe For Volume",
                subtitle = "Slide your finger on the right side to change volume",
                checked = settings.gestureVolumeEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(gestureVolumeEnabled = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Swipe For Brightness",
                subtitle = "Slide your finger on the left side to change screen brightness",
                checked = settings.gestureBrightnessEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(gestureBrightnessEnabled = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Swipe To Seek",
                subtitle = "Drag horizontally on the video to scrub through the episode",
                checked = settings.gestureHorizontalSeekEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(gestureHorizontalSeekEnabled = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Tap To Show Controls",
                subtitle = "A single tap shows or hides the player controls",
                checked = settings.singleTapTogglesControls,
                onCheckedChange = { settingsManager.update { s -> s.copy(singleTapTogglesControls = it) } }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Hide Controls After",
                subtitle = "How long the controls stay on screen while playing",
                value = settings.controlsAutoHideSec,
                range = 2f..30f,
                steps = 13,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(controlsAutoHideSec = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Vertical Swipe Opens Settings",
                subtitle = "Swipe up or down to open the quick player settings",
                checked = settings.swipeUpDownOpensSettings,
                onCheckedChange = { settingsManager.update { s -> s.copy(swipeUpDownOpensSettings = it) } }
            )
        }

        SettingsSection(title = "HOLD TO SPEED") {
            SettingSwitchRow(
                title = "Hold To Speed Up",
                subtitle = "Press and hold the video to play faster while your finger is down",
                checked = settings.holdToSpeedEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(holdToSpeedEnabled = it) } }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Hold Speed",
                subtitle = "Playback speed while holding the screen",
                value = settings.holdToSpeedMultiplier,
                valueRange = 1.25f..4f,
                steps = 10,
                valueLabel = { String.format(java.util.Locale.US, "%.2fx", it) },
                onValueChange = { settingsManager.updateHoldToSpeed(it, settings.holdToSpeedHaptic) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Vibrate When Speeding Up",
                subtitle = "Confirm with a gentle vibration pulse when hold-to-speed activates",
                checked = settings.holdToSpeedHaptic,
                onCheckedChange = { settingsManager.updateHoldToSpeed(settings.holdToSpeedMultiplier, it) }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Show Speed Badge",
                subtitle = "Display the current speed while you are holding",
                checked = settings.holdToSpeedShowBadge,
                onCheckedChange = { settingsManager.update { s -> s.copy(holdToSpeedShowBadge = it) } }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Hold Zone",
                subtitle = "Where a long press speeds playback up",
                currentValue = settings.holdToSpeedZone,
                options = listOf("Anywhere", "Left Half", "Right Half"),
                onSelect = { settingsManager.update { s -> s.copy(holdToSpeedZone = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Slow Down On The Other Half",
                subtitle = "Long press the opposite half of the video to play in slow motion instead",
                checked = settings.holdToSpeedSlowDown,
                onCheckedChange = { settingsManager.update { s -> s.copy(holdToSpeedSlowDown = it) } }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Slow-Motion Speed",
                subtitle = "Playback speed while holding the slow-down half",
                value = settings.holdToSpeedSlowMultiplier,
                valueRange = 0.25f..1f,
                steps = 14,
                valueLabel = { String.format(java.util.Locale.US, "%.2fx", it) },
                onValueChange = { settingsManager.update { s -> s.copy(holdToSpeedSlowMultiplier = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Ease Into The Speed",
                subtitle = "Ramp up instead of jumping straight to the multiplier",
                checked = settings.holdToSpeedRampUp,
                onCheckedChange = { settingsManager.update { s -> s.copy(holdToSpeedRampUp = it) } }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Speed Badge Position",
                subtitle = "Where the speed indicator appears while holding",
                currentValue = settings.holdToSpeedBadgePosition,
                options = listOf("Top", "Center", "Bottom"),
                onSelect = { settingsManager.update { s -> s.copy(holdToSpeedBadgePosition = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Keep The Speed After Release",
                subtitle = "Stay at the held speed when you lift your finger",
                checked = settings.holdToSpeedKeepAfterRelease,
                onCheckedChange = { settingsManager.update { s -> s.copy(holdToSpeedKeepAfterRelease = it) } }
            )
        }

        SettingsSection(title = "VIDEO OUTPUT") {
            SettingChoiceRow(
                title = "Video Fit",
                subtitle = "Fit keeps the whole frame visible, Cover fills the screen",
                currentValue = settings.videoFitMode,
                options = listOf("Fit", "Cover", "Stretch", "Zoom"),
                onSelect = { settingsManager.update { s -> s.copy(videoFitMode = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Keep Screen On",
                subtitle = "Stop the screen from sleeping while you watch",
                checked = settings.keepScreenOn,
                onCheckedChange = { settingsManager.update { s -> s.copy(keepScreenOn = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Background Playback",
                subtitle = "Keep audio playing when you leave the app",
                checked = settings.backgroundPlayback,
                onCheckedChange = { settingsManager.update { s -> s.copy(backgroundPlayback = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Picture In Picture",
                subtitle = "Keep watching in a small floating window",
                checked = settings.pipEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(pipEnabled = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Even Out Loud Volume",
                subtitle = "Reduce sudden loud moments, such as action scenes",
                checked = settings.loudnessNormalization,
                onCheckedChange = { settingsManager.update { s -> s.copy(loudnessNormalization = it) } }
            )
        }

        SettingsSection(title = "PLAYER LAYOUT & CUSTOMIZATION") {
            SettingSwitchRow(
                title = "Legacy Video Player Layout",
                subtitle = "Switch back to the classic player layout with extra controls and center lock banner",
                checked = settings.useLegacyPlayerLayout,
                onCheckedChange = { settingsManager.update { s -> s.copy(useLegacyPlayerLayout = it) } }
            )
            if (onOpenLayoutPlayground != null) {
                SettingsDivider()
                SettingActionRow(
                    title = "Open Video Player Layout Playground",
                    subtitle = "Move controls, hide or show buttons, change colors and preview live",
                    onClick = onOpenLayoutPlayground
                )
            }
        }

        SettingsSection(title = "PLAYER LOCK") {
            SettingSwitchRow(
                title = "Show Lock Button",
                subtitle = "Adds a padlock that hides the controls and blocks accidental taps",
                checked = settings.lockButtonEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(lockButtonEnabled = it) } }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Lock Icon Position",
                subtitle = "Position the unobtrusive lock button in the left or right letterbox margin",
                currentValue = settings.lockButtonSide,
                options = listOf("Left", "Right"),
                onSelect = { settingsManager.update { s -> s.copy(lockButtonSide = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Lock Automatically",
                subtitle = "Lock the screen on its own while you are watching",
                checked = settings.autoLockEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(autoLockEnabled = it) } }
            )
            SettingsDivider()
            IntSliderRow(
                title = "Auto Lock After",
                subtitle = "Idle time before the player locks itself",
                value = settings.autoLockDelaySec,
                range = 15f..300f,
                steps = 18,
                suffix = "s",
                onChange = { settingsManager.update { s -> s.copy(autoLockDelaySec = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Lock Blocks Touches",
                subtitle = "Ignore taps and swipes while locked so you cannot skip by accident",
                checked = settings.lockBlocksTouches,
                onCheckedChange = { settingsManager.update { s -> s.copy(lockBlocksTouches = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Vibrate On Lock",
                subtitle = "Feel a pulse when the player is locked or unlocked",
                checked = settings.lockHapticFeedback,
                onCheckedChange = { settingsManager.update { s -> s.copy(lockHapticFeedback = it) } }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Show Locked Indicator",
                subtitle = "Keep a small lock badge visible while locked",
                checked = settings.showLockedBadge,
                onCheckedChange = { settingsManager.update { s -> s.copy(showLockedBadge = it) } }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Rotation Lock",
                subtitle = "Which way the player is allowed to turn",
                currentValue = settings.orientationLockMode,
                options = listOf("Auto", "Landscape", "Portrait", "Lock Current"),
                onSelect = { settingsManager.update { s -> s.copy(orientationLockMode = it) } }
            )
        }

        SettingsSection(title = "GOOGLE CAST & SMART TV") {
            SettingSwitchRow(
                title = "Google Cast & Smart TV Streaming",
                subtitle = "Stream video directly to Google TV, Chromecast, or Android TV. Completely disabled by default.",
                checked = settings.chromecastEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(chromecastEnabled = it) } }
            )
        }

        SettingsSection(title = "2-WAY TRACKER SYNC & SCROBBLER") {
            SettingSwitchRow(
                title = "Enable 2-Way Tracker Sync",
                subtitle = "Synchronize watching progress and status with AniList / MAL. Disabled by default.",
                checked = settings.trackerSyncEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(trackerSyncEnabled = it) } }
            )
            if (settings.trackerSyncEnabled) {
                SettingsDivider()
                SettingSwitchRow(
                    title = "Auto-Scrobble Watched Episodes",
                    subtitle = "Automatically update online anime list when episode reaches completion threshold",
                    checked = settings.autoScrobbleEnabled,
                    onCheckedChange = { settingsManager.update { s -> s.copy(autoScrobbleEnabled = it) } }
                )
                SettingsDivider()
                IntSliderRow(
                    title = "Scrobble Threshold",
                    subtitle = "Percentage of episode watched before triggering auto-scrobble",
                    value = settings.scrobbleCompletionPercent,
                    range = 70f..95f,
                    steps = 4,
                    suffix = "%",
                    onChange = { settingsManager.update { s -> s.copy(scrobbleCompletionPercent = it) } }
                )
            }
        }

        SettingsSection(title = "DISCORD RICH PRESENCE") {
            SettingSwitchRow(
                title = "Discord Rich Presence",
                subtitle = "Broadcast currently watching anime and remaining time to Discord webhook",
                checked = settings.discordRpcEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(discordRpcEnabled = it) } }
            )
        }

        SettingsSection(title = "BACKGROUND AUDIO & PIP") {
            SettingSwitchRow(
                title = "Background Audio Playback",
                subtitle = "Keep audio playing seamlessly when screen is turned off or app is minimized",
                checked = settings.audioOnlyBackgroundEnabled,
                onCheckedChange = { settingsManager.update { s -> s.copy(audioOnlyBackgroundEnabled = it) } }
            )
        }

        SettingsSection(title = "RESET") {
            SettingActionRow(
                title = "Reset Player Preferences",
                subtitle = "Restores streaming, playback, gesture, video and lock settings to their defaults. Downloads, library and profile are kept.",
                destructive = true,
                onClick = {
                    settingsManager.resetPlayerPreferences()
                    Toast.makeText(context, "Player preferences reset", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Text(
            text = "Changes are saved the moment you make them.",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Integer slider with a suffix label, kept terse so the sections stay readable. */
@Composable
private fun IntSliderRow(
    title: String,
    subtitle: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    steps: Int,
    onChange: (Int) -> Unit
) {
    SettingSliderRow(
        title = title,
        subtitle = subtitle,
        value = value.toFloat(),
        valueRange = range,
        steps = steps,
        valueLabel = { "${it.roundToInt()}$suffix" },
        onValueChange = { onChange(it.roundToInt()) }
    )
}

private val SERVER_OPTIONS = listOf(
    "Mega (Recommended)" to "mega",
    "AllAnime" to "allanime",
    "AniEmbed" to "aniembed",
    "Viduki" to "viduki",
    "Web Embed (Fallback)" to "embed"
)

private val LANGUAGE_OPTIONS = listOf(
    "Sub (Original audio + subtitles)" to "sub",
    "Dub (English audio)" to "dub"
)
