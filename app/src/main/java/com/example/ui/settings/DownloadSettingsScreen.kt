package com.example.ui.settings

import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.AnimeRepository
import java.io.File
import java.util.Locale

@Composable
fun DownloadSettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    val storageUsed = remember {
        val internal = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val publicRoot = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Zenith-Anime"
        )
        formatBytes(directorySize(internal) + directorySize(publicRoot))
    }

    SettingsScaffold(
        title = "Downloads",
        subtitle = "Quality, storage and queueing",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "QUALITY AND FORMAT") {
            SettingChoiceRow(
                title = "Download quality",
                subtitle = "Pick the resolution used for new downloads",
                currentValue = settings.downloadQuality,
                options = listOf("1080p", "720p", "480p", "360p"),
                onSelect = { quality ->
                    repository.settingsManager.update { it.copy(downloadQuality = quality) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "File format",
                subtitle = "TV streams are saved as .ts, which VLC and MX Player can play",
                currentValue = settings.downloadPreferredContainer,
                options = listOf("Auto", "MP4", "TS"),
                onSelect = { container ->
                    repository.settingsManager.update { it.copy(downloadPreferredContainer = container) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Save subtitles",
                subtitle = "Keep an .srt or .vtt sidecar next to each episode",
                currentValue = settings.downloadSubtitles,
                options = listOf("English", "None"),
                onSelect = { subs ->
                    repository.settingsManager.update { it.copy(downloadSubtitles = subs) }
                }
            )
        }

        SettingsSection(title = "STORAGE") {
            SettingChoiceRow(
                title = "Where to save episodes",
                subtitle = "Episodes always play offline inside Zenith in-app; the folder only matters for files you want in other players",
                currentValue = settings.downloadLocation,
                options = listOf("In-App", "Downloads folder", "Custom folder (SAF)"),
                onSelect = { location ->
                    repository.settingsManager.update { it.copy(downloadLocation = location) }
                }
            )
            SettingsDivider()
            SettingActionRow(
                title = "Custom folder",
                subtitle = "Pick any folder on your phone or SD card for exported episodes",
                valueText = settings.downloadCustomFolderUri.ifBlank { "Not set" },
                onClick = {
                    Toast.makeText(
                        context,
                        "Choose the folder from the Downloads tab after your next download.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Also copy to public Downloads",
                subtitle = "Duplicates every finished episode into your shared Downloads folder",
                checked = settings.downloadExportAfterFinish,
                onCheckedChange = { enabled ->
                    repository.settingsManager.update { it.copy(downloadExportAfterFinish = enabled) }
                }
            )
        }

        SettingsSection(title = "QUEUE AND NETWORK") {
            SettingSwitchRow(
                title = "Wi-Fi only",
                subtitle = "Only download while connected to Wi-Fi",
                checked = settings.wifiOnlyDownloads,
                onCheckedChange = { enabled ->
                    repository.settingsManager.update { it.copy(wifiOnlyDownloads = enabled) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Parallel downloads",
                subtitle = "How many episodes download at the same time",
                value = settings.downloadConcurrency.toFloat(),
                valueRange = 1f..4f,
                steps = 2,
                valueLabel = { "${it.toInt()} at a time" },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(downloadConcurrency = value.toInt().coerceIn(1, 4)) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Queue next episodes",
                subtitle = "Automatically continue downloading the following episodes in order",
                value = settings.downloadAutoNextCount.toFloat(),
                valueRange = 0f..10f,
                steps = 9,
                valueLabel = { count ->
                    val n = count.toInt()
                    if (n == 0) "Off" else "Next $n episodes"
                },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(downloadAutoNextCount = value.toInt().coerceIn(0, 10)) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Retry attempts",
                subtitle = "How many times a failed episode is retried before giving up",
                value = settings.downloadRetryCount.toFloat(),
                valueRange = 0f..5f,
                steps = 4,
                valueLabel = { count ->
                    val n = count.toInt()
                    if (n == 0) "No retries" else "$n retries"
                },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(downloadRetryCount = value.toInt().coerceIn(0, 5)) }
                }
            )
        }

        SettingsSection(title = "AFTER WATCHING") {
            SettingSwitchRow(
                title = "Delete offline file when finished",
                subtitle = "Frees up space automatically once you complete an episode",
                checked = settings.downloadAutoDeleteWatched,
                onCheckedChange = { enabled ->
                    repository.settingsManager.update { it.copy(downloadAutoDeleteWatched = enabled) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Count as watched at",
                subtitle = "Progress percentage that marks an episode as finished",
                value = settings.markWatchedThresholdPercent.toFloat(),
                valueRange = 50f..100f,
                steps = 9,
                valueLabel = { "${it.toInt()}%" },
                onValueChange = { value ->
                    val rounded = (value.toInt() / 5) * 5
                    repository.settingsManager.update {
                        it.copy(markWatchedThresholdPercent = rounded.coerceIn(50, 100))
                    }
                }
            )
        }

        SettingsSection(title = "NOTIFICATIONS") {
            SettingSwitchRow(
                title = "Download progress",
                subtitle = "Show progress in the notification shade",
                checked = settings.downloadNotifications,
                onCheckedChange = { enabled ->
                    repository.settingsManager.update { it.copy(downloadNotifications = enabled) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Sound and vibration",
                subtitle = "Play a sound when a download finishes",
                checked = settings.downloadNotificationsSound,
                onCheckedChange = { enabled ->
                    repository.settingsManager.update { it.copy(downloadNotificationsSound = enabled) }
                }
            )
        }

        SettingsSection(title = "STORAGE USED") {
            SettingActionRow(
                title = "Space taken by episodes",
                subtitle = "Offline files stored by Zenith Anime",
                valueText = storageUsed,
                onClick = {
                    Toast.makeText(context, "Offline episodes use $storageUsed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/** Recursively totals the bytes of every readable file, ignoring missing or unreadable paths. */
private fun directorySize(dir: File?, depth: Int = 0): Long {
    if (dir == null || depth > 6 || !dir.exists() || !dir.isDirectory) return 0L
    val children = runCatching { dir.listFiles() }.getOrNull() ?: return 0L
    var total = 0L
    for (child in children) {
        total += runCatching {
            when {
                child.isDirectory -> directorySize(child, depth + 1)
                child.isFile -> child.length()
                else -> 0L
            }
        }.getOrDefault(0L)
    }
    return total
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}
