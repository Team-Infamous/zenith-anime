package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.WatchProgress
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    progress: WatchProgress,
    onPlayClick: () -> Unit,
    onDismissClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    displayTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val themeAccent = LocalThemeAccent.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "continue_card_scale"
    )

    Column(
        modifier = modifier
            .width(170.dp)
            .scale(scaleAnim)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = themeAccent),
                onClick = onPlayClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                }
            )
            .testTag("continue_card_${progress.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
        ) {
            AsyncImage(
                model = progress.coverImage,
                contentDescription = progress.animeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0x99000000), CircleShape)
                        .combinedClickable(
                            onClick = { onInfoClick?.invoke() ?: onLongClick?.invoke() }
                        )
                        .testTag("continue_card_info_${progress.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Anime Details",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color(0x99000000), CircleShape)
                        .combinedClickable(onClick = onDismissClick)
                        .testTag("continue_card_remove_${progress.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color(0xD9101418), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (progress.currentTimeSec == 0L) "Up Next: Ep ${progress.episodeNumber}" else "Episode ${progress.episodeNumber}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { if (progress.currentTimeSec == 0L) 0f else progress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = themeAccent,
                trackColor = Color(0x33FFFFFF),
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = displayTitle ?: progress.animeTitle,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (progress.currentTimeSec == 0L) "Ready to watch" else "${progress.formattedCurrentTime()} / ${progress.formattedDuration()}",
                color = if (progress.currentTimeSec == 0L) themeAccent else TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = if (progress.currentTimeSec == 0L) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
