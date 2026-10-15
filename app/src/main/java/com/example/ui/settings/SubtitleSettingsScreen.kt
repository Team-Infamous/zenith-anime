package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SubtitleSettingsScreen(
    repository: AnimeRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    val textColor = subtitleTextColorFor(settings.subtitleTextColor)
    val pillColor = subtitleBackgroundFor(settings.subtitleBackgroundColor)
    val previewAlignment = when (settings.subtitlePosition) {
        "Top" -> Alignment.TopCenter
        "Middle" -> Alignment.Center
        else -> Alignment.BottomCenter
    }
    val delayLabel = if (settings.subtitleDelayMs > 0) "+${settings.subtitleDelayMs} ms" else "${settings.subtitleDelayMs} ms"

    SettingsScaffold(
        title = "Subtitles",
        subtitle = "Language, appearance and timing",
        onBackClick = onBackClick,
        modifier = modifier
    ) {
        SettingsSection(title = "PREVIEW") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .align(previewAlignment)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(pillColor)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "This is how your subtitles will look.",
                        color = if (settings.subtitlesEnabled) textColor else Color.Gray,
                        fontSize = settings.subtitleFontSizeSp.sp,
                        fontWeight = if (settings.subtitleBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = if (settings.subtitleOutline) {
                                Shadow(color = Color.Black, blurRadius = 6f)
                            } else {
                                null
                            }
                        )
                    )
                }
            }
            Text(
                text = if (settings.subtitlesEnabled) {
                    "Preview uses your current size, colour, background, outline and position."
                } else {
                    "Subtitles are currently turned off, so nothing will show in the player."
                },
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        SettingsSection(title = "LANGUAGE") {
            SettingSwitchRow(
                title = "Show Subtitles",
                subtitle = "Turn subtitle tracks on in the player",
                checked = settings.subtitlesEnabled,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(subtitlesEnabled = value) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Default Subtitle Language",
                subtitle = "Preferred language picked automatically when a stream offers it",
                currentValue = settings.subtitleLanguage,
                options = listOf("English", "Spanish", "French", "Portuguese", "German", "Arabic", "Japanese"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(subtitleLanguage = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Look for Subtitles on the Server",
                subtitle = "Search the streaming server for its own subtitle tracks",
                checked = settings.subtitleFetchEnabled,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleFetchEnabled = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Auto-Match Subtitle Language",
                subtitle = "Automatically pick the track that matches the selected language",
                checked = settings.subtitleAutoMatch,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleAutoMatch = value) }
                }
            )
        }

        SettingsSection(title = "APPEARANCE") {
            SettingSliderRow(
                title = "Subtitle Size",
                subtitle = "Text size of every subtitle line",
                value = settings.subtitleFontSizeSp.toFloat(),
                valueRange = 10f..30f,
                steps = 19,
                valueLabel = { "${it.roundToInt()} sp" },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleFontSizeSp = value.roundToInt()) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Subtitle Colour",
                subtitle = "Colour of the subtitle text",
                currentValue = settings.subtitleTextColor,
                options = listOf("White", "Yellow", "Cyan", "Green"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(subtitleTextColor = value) }
                }
            )
            SettingsDivider()
            SettingChoiceRow(
                title = "Subtitle Background",
                subtitle = "Backing pill behind the text so it stays readable",
                currentValue = settings.subtitleBackgroundColor,
                options = listOf("None", "BlackSemi", "Black", "Grey"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(subtitleBackgroundColor = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Text Outline",
                subtitle = "Adds a shadow so subtitles stay readable on bright scenes",
                checked = settings.subtitleOutline,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleOutline = value) }
                }
            )
            SettingsDivider()
            SettingSwitchRow(
                title = "Bold Text",
                subtitle = "Render subtitle text in bold",
                checked = settings.subtitleBold,
                onCheckedChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleBold = value) }
                }
            )
        }

        SettingsSection(title = "POSITION AND TIMING") {
            SettingChoiceRow(
                title = "Subtitle Position",
                subtitle = "Where subtitles sit on the video",
                currentValue = settings.subtitlePosition,
                options = listOf("Bottom", "Middle", "Top"),
                onSelect = { value ->
                    repository.settingsManager.update { it.copy(subtitlePosition = value) }
                }
            )
            SettingsDivider()
            SettingSliderRow(
                title = "Subtitle Delay",
                subtitle = "Nudge subtitles earlier or later. Current value: $delayLabel",
                value = settings.subtitleDelayMs.toFloat(),
                valueRange = -3000f..3000f,
                steps = 59,
                valueLabel = { value ->
                    val ms = value.roundToInt()
                    if (ms > 0) "+$ms ms" else "$ms ms"
                },
                onValueChange = { value ->
                    repository.settingsManager.update { it.copy(subtitleDelayMs = value.roundToInt()) }
                }
            )
        }

        SettingsSection(title = "RESET") {
            SettingActionRow(
                title = "Reset Subtitle Settings",
                subtitle = "Restore language, appearance, position and timing defaults",
                onClick = {
                    repository.settingsManager.updateSubtitleSettings(
                        enabled = true,
                        language = "English",
                        fontSizeSp = 16,
                        textColor = "White",
                        backgroundColor = "BlackSemi",
                        outline = true,
                        bold = false,
                        position = "Bottom",
                        delayMs = 0
                    )
                    Toast.makeText(context, "Subtitle settings reset", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Text(
            text = "Subtitle tracks appear when the selected server provides them.",
            color = TextPrimary,
            fontSize = 11.sp
        )
    }
}

private fun subtitleTextColorFor(name: String): Color = when (name) {
    "Yellow" -> Color(0xFFFFEB3B)
    "Cyan" -> Color(0xFF00E5FF)
    "Green" -> Color(0xFF69F0AE)
    else -> Color.White
}

private fun subtitleBackgroundFor(name: String): Color = when (name) {
    "BlackSemi" -> Color(0xAA000000)
    "Black" -> Color(0xE6000000)
    "Grey" -> Color(0xAA444444)
    else -> Color.Transparent
}
