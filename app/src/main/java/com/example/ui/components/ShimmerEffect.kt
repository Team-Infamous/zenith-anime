package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * High-performance smooth shimmer effect for loading states,
 * calibrated to match the dark emerald aesthetic (#101218 / #161B26).
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat().coerceAtLeast(100f),
        targetValue = 2 * size.width.toFloat().coerceAtLeast(100f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerColors = listOf(
        Color(0xFF161B26),
        Color(0xFF242C3D),
        Color(0xFF161B26)
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat().coerceAtLeast(200f), size.height.toFloat().coerceAtLeast(200f))
        )
    ).onGloballyPositioned {
        size = it.size
    }
}
