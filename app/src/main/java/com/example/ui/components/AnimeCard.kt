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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Anime
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardWidth: Float? = null,
    rank: Int? = null,
    titlePreference: String = LocalAppSettings.current.titleLanguagePreference
) {
    val haptic = LocalHapticFeedback.current
    val themeAccent = LocalThemeAccent.current
    val appSettings = LocalAppSettings.current
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerShape = RoundedCornerShape(appSettings.cardCornerRadiusDp.coerceIn(4, 28).dp)

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    val widthMod = if (cardWidth != null) Modifier.width(cardWidth.dp) else Modifier.fillMaxWidth()

    Column(
        modifier = modifier
            .then(widthMod)
            .scale(scaleAnim)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = themeAccent),
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                }
            )
            .testTag("anime_card_${anime.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .shadow(
                    elevation = if (isPressed) 8.dp else 2.dp,
                    shape = cornerShape,
                    ambientColor = themeAccent.copy(alpha = 0.25f),
                    spotColor = themeAccent.copy(alpha = 0.2f)
                )
                .clip(cornerShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 0.5.dp,
                    color = if (isPressed) themeAccent.copy(alpha = 0.6f) else Color(0x1FFFFFFF),
                    shape = cornerShape
                )
        ) {
            AsyncImage(
                model = anime.coverImage,
                contentDescription = anime.displayTitle(titlePreference),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (rank != null || appSettings.showQualityBadgesOnCards) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rank != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$rank",
                                color = MaterialTheme.colorScheme.background,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    } else if (appSettings.showQualityBadgesOnCards) {
                        AgeRatingBadge(ageRating = anime.ageRating)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (appSettings.showQualityBadgesOnCards) {
                        QualityBadge(quality = anime.quality)
                    }
                }
            }

            if (appSettings.showRatingBadgesOnCards || appSettings.showEpisodeBadgesOnCards) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xE60B0E14))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appSettings.showRatingBadgesOnCards) {
                        RatingStars(rating = anime.rating)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (appSettings.showEpisodeBadgesOnCards) {
                        if (anime.availableEpisodes > 0) {
                            EpisodeCountPill(count = anime.availableEpisodes)
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x66000000))
                                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "TBA",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = anime.displayTitle(titlePreference),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
