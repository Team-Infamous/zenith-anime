package com.example.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.repository.AnimeRepository
import kotlin.math.roundToInt

@Composable
fun NotificationSettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    SettingsScaffold(
        title = "Notifications",
        subtitle = "New episodes and reminders",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "ALERTS") {
            SettingSwitchRow(
                title = "New episodes",
                subtitle = "Tell me when a new episode of a show in My List is out",
                checked = settings.newEpisodeNotifications,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(newEpisodeNotifications = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Airing reminders",
                subtitle = "Tell me when a show I follow starts airing",
                checked = settings.airingNotifications,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(airingNotifications = value) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Remind me early",
                subtitle = "How long before an episode airs to send the reminder",
                value = settings.notificationLeadMinutes.toFloat(),
                valueRange = 0f..60f,
                steps = 59,
                valueLabel = {
                    val minutes = it.roundToInt()
                    if (minutes == 0) "On time" else "$minutes min before"
                },
                onValueChange = { value ->
                    repository.settingsManager.update {
                        it.copy(notificationLeadMinutes = value.roundToInt())
                    }
                }
            )
        }

        SettingsSection(title = "DOWNLOADS") {
            SettingSwitchRow(
                title = "Download progress",
                subtitle = "Show download progress in the notification shade",
                checked = settings.downloadNotifications,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(downloadNotifications = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Download finished",
                subtitle = "Notify me when an episode finishes downloading",
                checked = settings.downloadCompleteNotifications,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(downloadCompleteNotifications = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Sound",
                subtitle = "Play a sound when a download finishes",
                checked = settings.downloadNotificationsSound,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(downloadNotificationsSound = value) }
                }
            )
        }

        SettingsSection(title = "PRIVACY") {
            SettingSwitchRow(
                title = "Hide from recent apps",
                subtitle = "Hide the app preview in the recent apps list",
                checked = settings.hideFromRecents,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(hideFromRecents = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Block screenshots",
                subtitle = "Block screenshots and screen recording",
                checked = settings.blockScreenshots,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(blockScreenshots = value) }
                }
            )
        }
    }
}
