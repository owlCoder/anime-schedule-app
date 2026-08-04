package com.owlcoder.animeschedule.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.ProvideMotionPolicy

/** Compact content shapes; large curvature is reserved for sheets and floating chrome. */
val AnimeScheduleShapes = Shapes().copy(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val PillShape = RoundedCornerShape(percent = 50)

fun accentPrimary(accent: AccentColor, dark: Boolean = false): Color = when (accent) {
    AccentColor.TELEGRAM_BLUE -> if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    AccentColor.PURPLE -> if (dark) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
    AccentColor.GREEN -> if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    AccentColor.ORANGE -> if (dark) Color(0xFFFF9F0A) else Color(0xFFFF9500)
    AccentColor.PINK -> if (dark) Color(0xFFFF375F) else Color(0xFFFF2D55)
    AccentColor.RED -> if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30)
    AccentColor.CYAN -> if (dark) Color(0xFF64D2FF) else Color(0xFF32ADE6)
    AccentColor.INDIGO -> if (dark) Color(0xFF5E5CE6) else Color(0xFF5856D6)
    AccentColor.TEAL -> if (dark) Color(0xFF40C8E0) else Color(0xFF30B0C7)
    AccentColor.YELLOW -> if (dark) Color(0xFFFFD60A) else Color(0xFFFFCC00)
    AccentColor.DEEP_PURPLE -> if (dark) Color(0xFFAC8E68) else Color(0xFF8E6E53)
}

private fun darkColors(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.20f).compositeOver(AppDarkGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFFEBEBF5).copy(alpha = 0.72f),
    onSecondary = Color.Black,
    secondaryContainer = AppDarkSecondary,
    onSecondaryContainer = Color.White,
    background = AppDarkBackground,
    onBackground = Color.White,
    surface = AppDarkGrouped,
    onSurface = Color.White,
    surfaceVariant = AppDarkSecondary,
    onSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.68f),
    surfaceContainerLowest = AppDarkBackground,
    surfaceContainerLow = AppDarkGrouped,
    surfaceContainer = AppDarkGrouped,
    surfaceContainerHigh = AppDarkElevated,
    surfaceContainerHighest = AppDarkSecondary,
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF545458).copy(alpha = 0.55f),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color.Black,
    inversePrimary = primary,
)

private fun lightColors(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.12f).compositeOver(AppLightGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFF3C3C43),
    onSecondary = Color.White,
    secondaryContainer = AppLightSecondary,
    onSecondaryContainer = Color.Black,
    background = AppLightBackground,
    onBackground = Color.Black,
    surface = AppLightGrouped,
    onSurface = Color.Black,
    surfaceVariant = AppLightSecondary,
    onSurfaceVariant = Color(0xFF3C3C43).copy(alpha = 0.72f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AppLightGrouped,
    surfaceContainer = AppLightGrouped,
    surfaceContainerHigh = AppLightElevated,
    surfaceContainerHighest = AppLightSecondary,
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3C3C43).copy(alpha = 0.20f),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE7E5),
    onErrorContainer = Color(0xFF7A120D),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    inversePrimary = primary,
)

private fun Color.compositeOver(background: Color): Color {
    val sourceAlpha = alpha
    return Color(
        red = red * sourceAlpha + background.red * (1f - sourceAlpha),
        green = green * sourceAlpha + background.green * (1f - sourceAlpha),
        blue = blue * sourceAlpha + background.blue * (1f - sourceAlpha),
        alpha = 1f,
    )
}

@Composable
fun AnimeScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme: ColorScheme = if (darkTheme) {
        darkColors(accentPrimary(accentColor, dark = true))
    } else {
        lightColors(accentPrimary(accentColor, dark = false))
    }

    // enableEdgeToEdge() follows the system theme by default. Keep system-bar icon
    // contrast synchronized with the in-app theme, including forced Light/Dark modes.
    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnimeScheduleShapes,
    ) {
        ProvideMotionPolicy(content = content)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
