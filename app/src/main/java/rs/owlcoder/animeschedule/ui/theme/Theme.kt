package rs.owlcoder.animeschedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode

private val LightColors = lightColorScheme(
    primary = TgBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEF0FB),
    onPrimaryContainer = TgBlueDark,
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EAF6),
    background = TgBackground,
    onBackground = Color(0xFF0F0F0F),
    surface = TgCard,
    onSurface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFFF1F1F3),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outline = Color(0xFFB0B0B0),
    outlineVariant = TgDivider,
    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFF5F5F5),
)

private val DarkColors = darkColorScheme(
    primary = TgBlueDarkMode,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A3A4A),
    onPrimaryContainer = TgBlueDarkMode,
    secondary = Color(0xFF7986CB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A2D4A),
    background = TgBackgroundDark,
    onBackground = Color(0xFFE8E8E8),
    surface = TgCardDark,
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = Color(0xFFEF9A9A),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF1A1A1A),
)

@Composable
fun AnimeScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
