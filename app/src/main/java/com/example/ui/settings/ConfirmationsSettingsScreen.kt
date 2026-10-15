package com.example.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.repository.AnimeRepository

/**
 * Confirmation and warning preferences. These gate the prompts shown before anything that cannot be
 * undone, so a user who wants a frictionless player can turn them off in one place.
 */
@Composable
fun ConfirmationsSettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    SettingsScaffold(
        title = "Confirmations",
        subtitle = "Prompts shown before irreversible actions",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "CONFIRMATIONS & WARNINGS") {
            SettingSwitchRow(
                title = "Confirm destructive actions",
                subtitle = "Ask before deletes, clears and resets",
                checked = settings.confirmDestructiveActions,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(confirmDestructiveActions = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Confirm before deleting downloads",
                subtitle = "Warn when a delete would remove an unfinished download",
                checked = settings.confirmBeforeDeletingDownloads,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(confirmBeforeDeletingDownloads = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Warn before auto-deleting watched",
                subtitle = "Tell me when a finished episode is removed automatically",
                checked = settings.warnBeforeAutoDeletingWatched,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(warnBeforeAutoDeletingWatched = value) }
                }
            )
        }
    }
}
