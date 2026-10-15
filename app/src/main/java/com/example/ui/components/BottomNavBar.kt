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
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.LocalThemeAccent
import com.example.ui.theme.TextSecondary

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
    modifier: Modifier = Modifier
) {
    val themeAccent = LocalThemeAccent.current
    val items = listOf(
        NavItem(Screen.Home.route, "Home", NavIcon.Vector(Icons.Filled.Home, Icons.Outlined.Home)),
        NavItem(Screen.Schedule.route, "Schedule", NavIcon.Vector(Icons.Filled.DateRange, Icons.Outlined.DateRange)),
        NavItem(Screen.MyList.route, "My List", NavIcon.Vector(Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)),
        NavItem(Screen.Download.route, "Download", NavIcon.DrawableRes(R.drawable.ic_nav_download_filled, R.drawable.ic_nav_download_outline)),
        NavItem(Screen.Profile.route, "Profile", NavIcon.Vector(Icons.Filled.Person, Icons.Outlined.PersonOutline))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .navigationBarsPadding()
    ) {
        androidx.compose.material3.HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) themeAccent else TextSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "nav_color_${item.label}"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
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
                        .padding(vertical = 4.dp)
                        .testTag("nav_item_${item.label.lowercase().replace(" ", "_")}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .scale(iconScale),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val icon = item.icon) {
                            is NavIcon.Vector -> {
                                Icon(
                                    imageVector = if (isSelected) icon.selected else icon.unselected,
                                    contentDescription = item.label,
                                    tint = animatedColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            is NavIcon.DrawableRes -> {
                                Icon(
                                    painter = painterResource(id = if (isSelected) icon.selectedRes else icon.unselectedRes),
                                    contentDescription = item.label,
                                    tint = animatedColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.label,
                        color = animatedColor,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Sleek active dot indicator
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = if (isSelected) themeAccent else androidx.compose.ui.graphics.Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
