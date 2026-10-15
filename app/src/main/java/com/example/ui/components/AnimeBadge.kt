package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.LocalThemeAccent

import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip

@Composable
fun AgeRatingBadge(ageRating: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ageRating,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun QualityBadge(quality: String = "HD", modifier: Modifier = Modifier) {
    val themeAccent = LocalThemeAccent.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, themeAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quality,
            color = themeAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LanguageBadge(text: String, isSelected: Boolean = false, modifier: Modifier = Modifier) {
    val themeAccent = LocalThemeAccent.current
    val bg = if (isSelected) themeAccent else Color(0x66000000)
    val textCol = if (isSelected) Color.Black else Color.White
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(0.5.dp, if (isSelected) themeAccent else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, AccentCyan.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OFFLINE",
            color = AccentCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = AccentYellow,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = String.format("%.2f", rating),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EpisodeCountPill(count: Int, modifier: Modifier = Modifier) {
    val themeAccent = LocalThemeAccent.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count EPS",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NextAiringBadge(
    episode: Int,
    timeUntilSec: Long?,
    atTimestamp: Long? = null,
    modifier: Modifier = Modifier
) {
    val themeAccent = LocalThemeAccent.current
    val countdownText = remember(timeUntilSec) {
        if (timeUntilSec == null || timeUntilSec <= 0) "Upcoming"
        else {
            val days = timeUntilSec / 86400
            val hours = (timeUntilSec % 86400) / 3600
            val mins = (timeUntilSec % 3600) / 60
            when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${mins}m"
                else -> "${mins}m"
            }
        }
    }

    Row(
        modifier = modifier
            .background(themeAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(themeAccent, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "Ep $episode: $countdownText",
            color = themeAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CurrentSeasonBadge(modifier: Modifier = Modifier) {
    val themeAccent = LocalThemeAccent.current
    Box(
        modifier = modifier
            .background(themeAccent, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CURRENT",
            color = Color.Black,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
