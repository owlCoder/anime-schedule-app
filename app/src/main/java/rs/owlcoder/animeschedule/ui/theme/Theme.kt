package rs.owlcoder.animeschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode

fun accentPrimary(accent: AccentColor, dark: Boolean = false): Color = when (accent) {
    AccentColor.TELEGRAM_BLUE -> if (dark) Color(0xFF1A8FCC) else Color(0xFF2AABEE)
    AccentColor.PURPLE        -> if (dark) Color(0xFF6A3FDD) else Color(0xFF8B5CF6)
    AccentColor.GREEN         -> if (dark) Color(0xFF00A846) else Color(0xFF22C55E)
    AccentColor.ORANGE        -> if (dark) Color(0xFFD45A00) else Color(0xFFF97316)
    AccentColor.PINK          -> if (dark) Color(0xFFC41570) else Color(0xFFEC4899)
    AccentColor.RED           -> if (dark) Color(0xFFCC2222) else Color(0xFFEF4444)
    AccentColor.CYAN          -> if (dark) Color(0xFF0891B2) else Color(0xFF06B6D4)
    AccentColor.INDIGO        -> if (dark) Color(0xFF3730A3) else Color(0xFF4F46E5)
    AccentColor.TEAL          -> if (dark) Color(0xFF0D7A6A) else Color(0xFF14B8A6)
    AccentColor.YELLOW        -> if (dark) Color(0xFFB48000) else Color(0xFFEAB308)
    AccentColor.DEEP_PURPLE   -> if (dark) Color(0xFF5B1FA8) else Color(0xFF7C3AED)
}

private fun darkColors(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f).compositeOver(Color(0xFF0D0D0D)),
    onPrimaryContainer = primary,
    secondary = Color(0xFF7986CB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A1D3A),
    background = TgBackgroundDark,
    onBackground = Color(0xFFE0E0E0),
    surface = TgSurfaceDark,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF909090),
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF222222),
    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFF2A0A0A),
    onErrorContainer = Color(0xFFEF9A9A),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF141414),
)

private fun lightColors(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.12f).compositeOver(Color.White),
    onPrimaryContainer = primary,
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF6),
    background = TgBackground,
    onBackground = Color(0xFF0F0F0F),
    surface = TgCard,
    onSurface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFFE4E4E8),
    onSurfaceVariant = Color(0xFF5A5A6A),
    outline = Color(0xFFB0B0B0),
    outlineVariant = TgDivider,
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF5F5F5),
)

// Blends a translucent color onto an opaque background (no BlendMode needed)
private fun Color.compositeOver(background: Color): Color {
    val fg = this
    val a = fg.alpha
    return Color(
        red   = fg.red   * a + background.red   * (1f - a),
        green = fg.green * a + background.green * (1f - a),
        blue  = fg.blue  * a + background.blue  * (1f - a),
        alpha = 1f
    )
}

@Composable
fun AnimeScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.TELEGRAM_BLUE,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val primary = accentPrimary(accentColor, dark = darkTheme)
    val colorScheme = if (darkTheme) darkColors(primary) else lightColors(primary)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
