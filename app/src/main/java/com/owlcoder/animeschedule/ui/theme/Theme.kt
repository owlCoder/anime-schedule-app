package com.owlcoder.animeschedule.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.presentation.components.ProvideMotionPolicy

/** Compact content shapes; larger curvature is reserved for sheets and floating chrome. */
val AnimeScheduleShapes = Shapes().copy(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(13.dp),
    large = RoundedCornerShape(17.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

val PillShape = RoundedCornerShape(percent = 50)

fun accentPrimary(accent: AccentColor, dark: Boolean = false): Color = when (accent) {
    AccentColor.TELEGRAM_BLUE -> if (dark) Color(0xFF64B5E8) else Color(0xFF087EBA)
    AccentColor.PURPLE -> if (dark) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    AccentColor.GREEN -> if (dark) Color(0xFF75DC91) else Color(0xFF087F3D)
    AccentColor.ORANGE -> if (dark) Color(0xFFFFB77A) else Color(0xFF9A4300)
    AccentColor.PINK -> if (dark) Color(0xFFFFB0C8) else Color(0xFF9B1746)
    AccentColor.RED -> if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
    AccentColor.CYAN -> if (dark) Color(0xFF6DDAF5) else Color(0xFF006877)
    AccentColor.INDIGO -> if (dark) Color(0xFFBFC4FF) else Color(0xFF4F5FBA)
    AccentColor.TEAL -> if (dark) Color(0xFF70DBC7) else Color(0xFF006A5B)
    AccentColor.YELLOW -> if (dark) Color(0xFFEFC238) else Color(0xFF765900)
    AccentColor.DEEP_PURPLE -> if (dark) Color(0xFFE0B9FF) else Color(0xFF7A3DA0)
}

private fun darkColors(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = readableOn(primary, dark = true),
    primaryContainer = primary.copy(alpha = 0.20f).compositeOver(AppDarkGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF293041),
    secondaryContainer = Color(0xFF40485A),
    onSecondaryContainer = Color(0xFFDAE2F9),
    background = AppDarkBackground,
    onBackground = Color.White,
    surface = AppDarkGrouped,
    onSurface = Color.White,
    surfaceVariant = AppDarkSecondary,
    onSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.60f),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = AppDarkGrouped,
    surfaceContainer = AppDarkGrouped,
    surfaceContainerHigh = AppDarkElevated,
    surfaceContainerHighest = AppDarkSecondary,
    outline = Color(0xFF545458),
    outlineVariant = Color(0xFF545458).copy(alpha = 0.48f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE6E1E6),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = primary,
)

private fun lightColors(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = readableOn(primary, dark = false),
    primaryContainer = primary.copy(alpha = 0.12f).compositeOver(AppLightGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFF5D5F72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1E2F2),
    onSecondaryContainer = Color(0xFF191A2B),
    background = AppLightBackground,
    onBackground = Color.Black,
    surface = AppLightGrouped,
    onSurface = Color.Black,
    surfaceVariant = AppLightSecondary,
    onSurfaceVariant = Color(0xFF3C3C43).copy(alpha = 0.60f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AppLightGrouped,
    surfaceContainer = AppLightGrouped,
    surfaceContainerHigh = AppLightElevated,
    surfaceContainerHighest = AppLightSecondary,
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF3C3C43).copy(alpha = 0.16f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2F3035),
    inverseOnSurface = Color(0xFFF1EFF4),
    inversePrimary = primary,
)

private fun readableOn(color: Color, dark: Boolean): Color {
    val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (luminance > if (dark) 0.48f else 0.62f) Color(0xFF1A1B20) else Color.White
}

private fun Color.compositeOver(background: Color): Color {
    val alpha = this.alpha
    return Color(
        red = red * alpha + background.red * (1f - alpha),
        green = green * alpha + background.green * (1f - alpha),
        blue = blue * alpha + background.blue * (1f - alpha),
        alpha = 1f,
    )
}

private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val container = accent.copy(alpha = if (dark) 0.20f else 0.12f)
        .compositeOver(if (dark) surfaceContainer else surface)
    return copy(
        primary = accent,
        onPrimary = readableOn(accent, dark),
        primaryContainer = container,
        onPrimaryContainer = accent,
        inversePrimary = accent,
    )
}

@Composable
fun AnimeScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.INDIGO,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) {
        darkColors(accentPrimary(accentColor, dark = true))
    } else {
        lightColors(accentPrimary(accentColor, dark = false))
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnimeScheduleShapes,
    ) {
        ProvideMotionPolicy(content = content)
    }
}
