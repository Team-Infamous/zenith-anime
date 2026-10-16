package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.navigation.Screen
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextSecondary

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

sealed class NavIcon {
    data class Vector(val selected: ImageVector, val unselected: ImageVector) : NavIcon()
    data class DrawableRes(val selectedRes: Int, val unselectedRes: Int) : NavIcon()
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: NavIcon
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    avatarModel: Any? = null,
    modifier: Modifier = Modifier
) {
    val themeAccent = LocalThemeAccent.current
    val appSettings = LocalAppSettings.current
    val isAmoled = appSettings.amoledMode
    val reducedMotion = LocalReducedMotion.current
    val navStyle = appSettings.navBarStyle

    val isCompact = navStyle.equals("Compact", ignoreCase = true)
    val isFloating = navStyle.equals("Floating", ignoreCase = true)

    val outerHorizontalPadding = when {
        isFloating -> 24.dp
        isCompact -> 16.dp
        else -> 16.dp
    }
    val outerVerticalPadding = when {
        isFloating -> 10.dp
        isCompact -> 6.dp
        else -> 8.dp
    }
    val barHeight = when {
        isCompact -> 52.dp
        else -> 64.dp
    }
    val barShape = when {
        isFloating -> RoundedCornerShape(32.dp)
        isCompact -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(24.dp)
    }
    val containerBg = if (isAmoled) Color(0xF2050507) else Color(0xEE121622)

    val items = listOf(
        NavItem(Screen.Home.route, "Home", NavIcon.Vector(Icons.Filled.Home, Icons.Outlined.Home)),
        NavItem(Screen.Schedule.route, "Schedule", NavIcon.Vector(Icons.Filled.DateRange, Icons.Outlined.DateRange)),
        NavItem(Screen.MyList.route, "My List", NavIcon.Vector(Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)),
        NavItem(Screen.Download.route, "Download", NavIcon.DrawableRes(R.drawable.ic_nav_download_filled, R.drawable.ic_nav_download_outline)),
        NavItem(Screen.Profile.route, "Profile", NavIcon.Vector(Icons.Filled.Person, Icons.Outlined.PersonOutline))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = if (isFloating) 16.dp else 12.dp,
                    shape = barShape,
                    ambientColor = themeAccent.copy(alpha = 0.25f),
                    spotColor = themeAccent.copy(alpha = 0.35f)
                )
                .clip(barShape)
                .background(containerBg)
                .border(
                    width = 0.5.dp,
                    color = if (isAmoled) Color(0x26FFFFFF) else Color(0x33FFFFFF),
                    shape = barShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val animatedColor by animateColorAsState(
                        targetValue = if (isSelected) themeAccent else TextSecondary,
                        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
                        label = "nav_color_${item.label}"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected && !reducedMotion) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "nav_scale_${item.label}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigate(item.route) }
                            .padding(vertical = if (isCompact) 2.dp else 4.dp)
                            .testTag("nav_item_${item.label.lowercase().replace(" ", "_")}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 22.dp else 26.dp)
                                .scale(iconScale),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.route == Screen.Profile.route && avatarModel != null) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompact) 20.dp else 24.dp)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.dp,
                                            color = if (isSelected) themeAccent else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(if (isSelected) 1.5.dp else 0.dp)
                                        .clip(CircleShape)
                                ) {
                                    AsyncImage(
                                        model = avatarModel,
                                        contentDescription = item.label,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                when (val icon = item.icon) {
                                    is NavIcon.Vector -> {
                                        Icon(
                                            imageVector = if (isSelected) icon.selected else icon.unselected,
                                            contentDescription = item.label,
                                            tint = animatedColor,
                                            modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
                                        )
                                    }
                                    is NavIcon.DrawableRes -> {
                                        Icon(
                                            painter = painterResource(id = if (isSelected) icon.selectedRes else icon.unselectedRes),
                                            contentDescription = item.label,
                                            tint = animatedColor,
                                            modifier = Modifier.size(if (isCompact) 20.dp else 22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!isCompact) {
                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = item.label,
                                color = animatedColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Modern luminous active glow pill
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 16.dp else 0.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    if (isSelected) themeAccent else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}
