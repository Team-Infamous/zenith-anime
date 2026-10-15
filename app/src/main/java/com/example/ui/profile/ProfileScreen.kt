package com.example.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.provider.ProviderManager
import com.example.data.repository.AnimeRepository
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    repository: AnimeRepository,
    onMalSyncClick: () -> Unit = {},
    onPlayerSettingsClick: () -> Unit = {},
    onSubtitleSettingsClick: () -> Unit = {},
    onDownloadSettingsClick: () -> Unit = {},
    onLibrarySettingsClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onConfirmationsSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val themeAccent = LocalThemeAccent.current

    val settings by repository.settings.collectAsState(initial = repository.settingsManager.getSettings())

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDataManagementDialog by remember { mutableStateOf(false) }
    var showPlayerSettingsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirmation by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val username = settings.username
    val selectedServer = settings.defaultServer
    val selectedAudio = settings.defaultLanguage
    val autoplayEnabled = settings.autoplay
    val autoNextEnabled = settings.autoNextEpisode
    val autoSkipIntroEnabled = settings.autoSkipIntro
    val seekIntervalSec = settings.seekDurationSec
    val defaultSpeed = settings.defaultPlaybackSpeed
    val selectedThemeColor = settings.themeAccent
    val incognitoMode = settings.incognitoMode
    val amoledMode = settings.amoledMode

    var importJsonText by remember { mutableStateOf("") }
    var backupStatusMessage by remember { mutableStateOf("") }

    val watchHistory by repository.allHistory.collectAsState(initial = emptyList())
    val favorites by repository.favorites.collectAsState(initial = emptyList())
    val downloads by repository.allDownloads.collectAsState(initial = emptyList())

    val providerManager = remember { ProviderManager() }

    fun persistAvatar(uri: android.net.Uri) {
        scope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val previous = repository.settingsManager.getSettings().avatarPath
                    val target = File(context.filesDir, "profile_avatar_" + System.currentTimeMillis() + ".jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (previous != null && previous != target.absolutePath) {
                        File(previous).takeIf { it.exists() }?.delete()
                    }
                    target.absolutePath
                } catch (_: Throwable) {
                    null
                }
            }
            if (savedPath != null) {
                repository.settingsManager.updateAvatarPath(savedPath)
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not read that image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeAvatar() {
        val previous = repository.settingsManager.getSettings().avatarPath
        scope.launch {
            withContext(Dispatchers.IO) {
                if (previous != null) {
                    File(previous).takeIf { it.exists() }?.delete()
                }
            }
            repository.settingsManager.updateAvatarPath(null)
            Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            persistAvatar(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_zenith_logo),
                contentDescription = "Zenith Anime Emblem",
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Profile & Settings",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = { showEditProfileDialog = true },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEditProfileDialog = true
                    }
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.5.dp, themeAccent, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = settings.avatarPath?.let { File(it) }
                        ?: "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300&auto=format&fit=crop&q=80",
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .background(themeAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = username,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (incognitoMode) "Incognito Active • Local-First" else "Ad-Free • Tracker-Free Streaming",
                    color = themeAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${watchHistory.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Watched",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${favorites.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Favorites",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .width(1.dp)
                    .background(DarkBorder)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${downloads.size}",
                    color = themeAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Edit,
                title = "Edit Profile",
                onClick = { showEditProfileDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.ColorLens,
                title = "Theme & Appearance ($selectedThemeColor)",
                onClick = { showThemeDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.PlayCircleOutline,
                title = "Player Settings",
                onClick = onPlayerSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Subtitles,
                title = "Subtitle Settings",
                onClick = onSubtitleSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Download,
                title = "Download Settings",
                onClick = onDownloadSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.VideoLibrary,
                title = "Library & Series Settings",
                onClick = onLibrarySettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Notifications,
                title = "Notification Settings",
                onClick = onNotificationSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.ReportProblem,
                title = "Confirmations & Warnings",
                onClick = onConfirmationsSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.SyncAlt,
                title = "MAL / AniList Watchlist Sync",
                onClick = onMalSyncClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Security,
                title = "Privacy, Ad-Block & Anti-Tracking",
                onClick = { showPrivacyDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.Storage,
                title = "Data Management (Backup / Import)",
                onClick = { showDataManagementDialog = true },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val json = repository.exportBackupJson()
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "Quick Export: Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.Storage,
                title = "Clear Watch History",
                onClick = {
                    if (settings.confirmDestructiveActions) {
                        showClearHistoryConfirmation = true
                    } else {
                        scope.launch {
                            repository.clearHistory()
                            Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "About Zenith Anime",
                onClick = { showAboutDialog = true }
            )

            ProfileMenuItem(
                icon = Icons.Default.DeleteSweep,
                title = "Reset All Application Data",
                titleColor = AccentRed,
                iconColor = AccentRed,
                onClick = { showResetConfirmationDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Clean Anime Streaming Engine • Zero Tracking",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Version 2.0.0 Pro • 5 Multi-Servers Active",
                color = Color(0xFF475569),
                fontSize = 11.sp
            )
        }
    }

    if (showThemeDialog) {
        val themes = listOf(
            "Emerald" to Color(0xFF00D26A),
            "Cyan" to Color(0xFF00E5FF),
            "Sakura" to Color(0xFFFF4081),
            "Violet" to Color(0xFFB388FF),
            "Amber" to Color(0xFFFFB300),
            "Crimson" to Color(0xFFFF5252)
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = DarkSurface,
            title = { Text("Accent Color Theme", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your personal accent styling:", color = TextSecondary, fontSize = 12.sp)
                    themes.forEach { (name, color) ->
                        val isSelected = selectedThemeColor == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .clickable {
                                    repository.settingsManager.updateThemeAccent(name)
                                    Toast.makeText(context, "Theme set to $name", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                color = if (isSelected) color else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Text("Active", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    SettingSwitchRow(
                        title = "AMOLED True Black Mode",
                        checked = amoledMode,
                        onCheckedChange = {
                            repository.settingsManager.updateAmoledMode(it)
                            Toast.makeText(context, if (it) "AMOLED mode enabled" else "AMOLED mode disabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = DarkSurface,
            title = { Text("Privacy & Anti-Tracking", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F261B))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("Ad & Popup Blocker: ACTIVE", color = themeAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Malicious popup windows, external scam redirects, and third-party advertising scripts are blocked at the network interceptor level.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "Incognito Mode (Do not save history)",
                        checked = incognitoMode,
                        onCheckedChange = {
                            repository.settingsManager.updateIncognitoMode(it)
                            Toast.makeText(context, if (it) "Incognito mode enabled" else "Incognito mode disabled", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                    Text("Zero Cloud Tracking:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "• No account required\n• No telemetry identifiers\n• No location access\n• 100% device-local Room database",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPlayerSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerSettingsDialog = false },
            containerColor = DarkSurface,
            title = { Text("Player & Server Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Default Streaming Server:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    providerManager.getAvailableProviders().forEach { provider ->
                        val isSelected = selectedServer == provider.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themeAccent.copy(alpha = 0.15f) else DarkSurfaceVariant)
                                .clickable {
                                    repository.settingsManager.updateDefaultServer(provider.id)
                                    Toast.makeText(context, "Default set to ${provider.id}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${provider.id}: ${provider.displayName}",
                                color = if (isSelected) themeAccent else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Box(modifier = Modifier.size(8.dp).background(themeAccent, CircleShape))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Default Audio Language:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { repository.settingsManager.updateDefaultLanguage("sub") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAudio == "sub") themeAccent else DarkSurfaceVariant,
                                contentColor = if (selectedAudio == "sub") DarkBackground else TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SUBTITLES (SUB)")
                        }
                        Button(
                            onClick = { repository.settingsManager.updateDefaultLanguage("dub") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedAudio == "dub") themeAccent else DarkSurfaceVariant,
                                contentColor = if (selectedAudio == "dub") DarkBackground else TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DUBBED (DUB)")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Double-Tap Seek Interval:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            val isSel = seekIntervalSec == sec
                            Button(
                                onClick = { repository.settingsManager.updateSeekInterval(sec) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) themeAccent else DarkSurfaceVariant,
                                    contentColor = if (isSel) DarkBackground else TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${sec}s", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Default Playback Speed:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                            val isSel = defaultSpeed == spd
                            Button(
                                onClick = { repository.settingsManager.updateDefaultSpeed(spd) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) themeAccent else DarkSurfaceVariant,
                                    contentColor = if (isSel) DarkBackground else TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${spd}x", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        title = "Autoplay Video Stream",
                        checked = autoplayEnabled,
                        onCheckedChange = { repository.settingsManager.updateAutoplay(it) }
                    )
                    SettingSwitchRow(
                        title = "Auto-Next Episode Countdown",
                        checked = autoNextEnabled,
                        onCheckedChange = { repository.settingsManager.updateAutoNext(it) }
                    )
                    SettingSwitchRow(
                        title = "Auto-Skip Opening/Intro (+85s)",
                        checked = autoSkipIntroEnabled,
                        onCheckedChange = { repository.settingsManager.updateAutoSkipIntro(it) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPlayerSettingsDialog = false
                        Toast.makeText(context, "Player preferences saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(username) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = DarkSurface,
            title = { Text("Edit Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Profile Picture", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (settings.avatarPath != null) {
                            Button(
                                onClick = { removeAvatar() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = AccentRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Remove Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.settingsManager.updateProfile(tempName)
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showDataManagementDialog) {
        AlertDialog(
            onDismissRequest = { showDataManagementDialog = false },
            containerColor = DarkSurface,
            title = { Text("Backup & Data Portability", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Zenith Anime is strictly local-first. Your favorites, watch progress, and history never leave your device without your explicit export.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val json = repository.exportBackupJson()
                                clipboardManager.setText(AnnotatedString(json))
                                backupStatusMessage = "Backup JSON (${json.length} chars) copied to clipboard"
                                Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = DarkBackground),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup JSON (Copy)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste Backup JSON to Import/Merge") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    if (importJsonText.isNotBlank()) {
                                        val res = repository.importBackupJson(importJsonText, mode = "MERGE")
                                        backupStatusMessage = res
                                        Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent.copy(alpha = 0.2f), contentColor = themeAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Smart Merge", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    if (importJsonText.isNotBlank()) {
                                        val res = repository.importBackupJson(importJsonText, mode = "REPLACE")
                                        backupStatusMessage = res
                                        Toast.makeText(context, res, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331E24), contentColor = AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Replace Data", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (backupStatusMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = backupStatusMessage,
                            color = themeAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataManagementDialog = false }) {
                    Text("Close", color = themeAccent)
                }
            }
        )
    }

    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            containerColor = DarkSurface,
            title = { Text("Reset Application Data?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will erase your local watch history, favorites, saved watchlist, and downloads from this device. This action cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.resetAllData()
                            showResetConfirmationDialog = false
                            Toast.makeText(context, "All application data reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryConfirmation && settings.confirmDestructiveActions) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmation = false },
            containerColor = DarkSurface,
            title = { Text("Clear Watch History?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This removes the " + watchHistory.size +
                        " episode" + (if (watchHistory.size == 1) "" else "s") +
                        " in your watch history, so Continue Watching starts over. Episode downloads, favorites and your watchlist are kept. This cannot be undone.",
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.clearHistory()
                            showClearHistoryConfirmation = false
                            Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextPrimary)
                ) {
                    Text("Clear History", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = DarkSurface,
            title = { Text("About Zenith Anime", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Zenith Anime is a privacy-first, local-first anime streaming application.",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Zero Cloud: No accounts, no telemetry, no tracking servers.\n• 5 Multi-Server streaming providers (TryEmbed, MegaPlay, VidLink, Kaido, FastCloud).\n• Automatic ad-blocking and redirect protection.\n• Full offline library, schedule and backup portability.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = themeAccent)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    titleColor: Color = TextPrimary,
    iconColor: Color = TextSecondary,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "menu_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            color = titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DarkBorder,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeAccent = LocalThemeAccent.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = themeAccent,
                checkedTrackColor = themeAccent.copy(alpha = 0.35f)
            )
        )
    }
}
