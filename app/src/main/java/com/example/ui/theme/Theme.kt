package com.example.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.data.model.AppSettings

val LocalThemeAccent = compositionLocalOf { AnimeGreen }
val LocalAppSettings = compositionLocalOf { AppSettings() }
val LocalReducedMotion = compositionLocalOf { false }

fun getThemeAccentColor(accentName: String): Color {
    return when (accentName.lowercase()) {
        "cyan" -> Color(0xFF00E5FF)
        "sakura" -> Color(0xFFFF4081)
        "violet" -> Color(0xFFB388FF)
        "amber" -> Color(0xFFFFB300)
        "crimson" -> Color(0xFFFF5252)
        "indigo" -> Color(0xFF536DFE)
        "gold" -> Color(0xFFFFD54F)
        else -> AnimeGreen
    }
}

/**
 * Checks whether animations should be reduced due to either the user's app preference or
 * the system-level `ANIMATOR_DURATION_SCALE == 0` accessibility setting.
 */
@Composable
fun rememberIsReducedMotion(appSettingReduceAnimations: Boolean): Boolean {
    val context = LocalContext.current
    val systemReduced = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale <= 0f
        } catch (_: Throwable) {
            false
        }
    }
    return appSettingReduceAnimations || systemReduced
}

@Composable
fun MyApplicationTheme(
    themeAccent: String = "Emerald",
    amoled: Boolean = false,
    appSettings: AppSettings = AppSettings(themeAccent = themeAccent, amoledMode = amoled),
    content: @Composable () -> Unit
) {
    val accentColor = getThemeAccentColor(themeAccent)
    val bgColor = if (amoled) Color.Black else DarkBackground
    val surfaceColor = if (amoled) Color(0xFF0A0A0A) else DarkSurface
    val surfaceVarColor = if (amoled) Color(0xFF141414) else DarkSurfaceVariant
    val reducedMotion = rememberIsReducedMotion(appSettings.reduceAnimations)

    val currentDensity = LocalDensity.current
    val customDensity = remember(currentDensity, appSettings.fontScaleMultiplier) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * appSettings.fontScaleMultiplier.coerceIn(0.85f, 1.25f)
        )
    }

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = bgColor,
        primaryContainer = accentColor.copy(alpha = 0.2f),
        onPrimaryContainer = accentColor,
        secondary = accentColor,
        onSecondary = bgColor,
        tertiary = AccentYellow,
        background = bgColor,
        onBackground = TextPrimary,
        surface = surfaceColor,
        onSurface = TextPrimary,
        surfaceVariant = surfaceVarColor,
        onSurfaceVariant = TextSecondary,
        outline = DarkBorder,
        error = AccentRed,
        onError = TextPrimary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = bgColor.toArgb()
                window.navigationBarColor = bgColor.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalThemeAccent provides accentColor,
        LocalAppSettings provides appSettings,
        LocalReducedMotion provides reducedMotion,
        LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
