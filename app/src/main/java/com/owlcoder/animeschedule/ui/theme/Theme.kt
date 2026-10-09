package com.owlcoder.animeschedule.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.owlcoder.animeschedule.domain.model.isDarkAt
import java.time.LocalTime
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.presentation.components.ProvideMotionPolicy
import com.owlcoder.animeschedule.domain.model.ThemeOptions
import com.owlcoder.animeschedule.domain.model.ThemePalette
import androidx.compose.ui.graphics.lerp
import com.owlcoder.animeschedule.presentation.components.MotionPolicy
import com.owlcoder.animeschedule.presentation.components.rememberMotionPolicy
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/** Compact content shapes; large curvature is reserved for sheets and floating chrome. */
val AnimeScheduleShapes = Shapes().copy(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val LocalCompactLayout = compositionLocalOf { false }

/** Resolved mode, including system/scheduled themes and dynamic colors. Cards retain their tone. */
val LocalAmoledDark = compositionLocalOf { false }

val PillShape = RoundedCornerShape(percent = 50)

internal fun Color.onAccent(): Color = if (luminance() > 0.179f) Color.Black else Color.White

private fun darkColors(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = primary.onAccent(),
    primaryContainer = primary.copy(alpha = 0.18f).compositeOver(AppDarkGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFFD1D1D6),
    onSecondary = Color.Black,
    secondaryContainer = AppDarkSecondary,
    onSecondaryContainer = Color.White,
    background = AppDarkBackground,
    onBackground = Color.White,
    surface = AppDarkGrouped,
    onSurface = Color(0xFFF2F2F4),
    surfaceVariant = AppDarkSecondary,
    onSurfaceVariant = Color(0xFFB2B2B8),
    surfaceContainerLowest = AppDarkBackground,
    surfaceContainerLow = AppDarkGrouped,
    surfaceContainer = AppDarkGrouped,
    surfaceContainerHigh = AppDarkElevated,
    surfaceContainerHighest = AppDarkSecondary,
    outline = Color(0xFF7C7C82),
    outlineVariant = Color(0xFF3D3D42),
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
    onPrimary = primary.onAccent(),
    primaryContainer = primary.copy(alpha = 0.14f).compositeOver(AppLightGrouped),
    onPrimaryContainer = primary,
    secondary = Color(0xFF3C3C43),
    onSecondary = Color.White,
    secondaryContainer = AppLightSecondary,
    onSecondaryContainer = Color.Black,
    background = AppLightBackground,
    onBackground = Color(0xFF10131A),
    surface = AppLightGrouped,
    onSurface = Color(0xFF141820),
    surfaceVariant = AppLightSecondary,
    onSurfaceVariant = Color(0xFF5E6675),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AppLightGrouped,
    surfaceContainer = AppLightGrouped,
    surfaceContainerHigh = AppLightElevated,
    surfaceContainerHighest = AppLightSecondary,
    outline = Color(0xFF8B94A3),
    outlineVariant = Color(0xFF5E6675).copy(alpha = 0.24f),
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

/** Palette roles are shared by cards, sheets and chrome rather than hard-coded by each view. */
internal fun buildAppColorScheme(dark: Boolean, options: ThemeOptions): ColorScheme {
    val seed = when (options.palette) {
        ThemePalette.CLASSIC -> Color(if (dark) 0xFF3D8FD6 else 0xFF007AFF)
        ThemePalette.MIDNIGHT -> Color(if (dark) 0xFFB6B3FF else 0xFF5954AD)
        ThemePalette.SAKURA -> Color(if (dark) 0xFFFFADC4 else 0xFFAC345C)
        ThemePalette.FOREST -> Color(if (dark) 0xFF9BD6AE else 0xFF256A43)
        ThemePalette.SAND -> Color(if (dark) 0xFFEAC58D else 0xFF80551D)
        ThemePalette.OCEAN -> Color(if (dark) 0xFF80D9ED else 0xFF00677A)
        ThemePalette.LAVENDER -> Color(if (dark) 0xFFD2BAFF else 0xFF6B489E)
        ThemePalette.EMBER -> Color(if (dark) 0xFFFFB18C else 0xFF9D441C)
        ThemePalette.ICE -> Color(if (dark) 0xFFADC9FF else 0xFF315D9C)
        ThemePalette.COFFEE -> Color(if (dark) 0xFFDDBAA4 else 0xFF79513B)
        ThemePalette.NEON -> Color(if (dark) 0xFFB3E778 else 0xFF486C16)
        ThemePalette.RUBY -> Color(if (dark) 0xFFFFAFB6 else 0xFFAA2941)
        ThemePalette.AMETHYST -> Color(if (dark) 0xFFE1B0F5 else 0xFF85429D)
        ThemePalette.MINT -> Color(if (dark) 0xFF8DDBC7 else 0xFF146C58)
        ThemePalette.GOLD -> Color(if (dark) 0xFFF0D17C else 0xFF806116)
    }
    val scheme = if (dark) darkColors(seed) else lightColors(seed)
    val tinted = if (options.palette == ThemePalette.CLASSIC) scheme else scheme.copy(
        background = lerp(if (dark) Color(0xFF101116) else Color(0xFFF7F7FA), seed, if (dark) 0.05f else 0.04f),
        surface = lerp(if (dark) Color(0xFF18191F) else Color.White, seed, 0.04f),
        surfaceContainerLow = lerp(if (dark) Color(0xFF18191F) else Color.White, seed, 0.04f),
        surfaceContainer = lerp(if (dark) Color(0xFF1E1F25) else Color(0xFFF9F9FC), seed, 0.05f),
        surfaceContainerHigh = lerp(if (dark) Color(0xFF24252C) else Color(0xFFF4F4F8), seed, 0.06f),
        surfaceContainerHighest = lerp(if (dark) Color(0xFF2D2E36) else Color(0xFFE8E9EF), seed, 0.07f),
        surfaceVariant = lerp(if (dark) Color(0xFF2D2E36) else Color(0xFFE8E9EF), seed, 0.07f),
    )
    return enhanceScheme(tinted, dark, options)
}

private fun enhanceScheme(scheme: ColorScheme, dark: Boolean, options: ThemeOptions): ColorScheme {
    val background = if (dark && options.amoled) Color.Black else scheme.background
    // Primary is also used as text on content surfaces, so maintain readable contrast there.
    val surfaces = listOf(scheme.surface, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.primaryContainer)
    val reference = if (dark) surfaces.maxBy { it.luminance() } else surfaces.minBy { it.luminance() }
    var primary = scheme.primary
    repeat(20) {
        val contrast = (maxOf(primary.luminance(), reference.luminance()) + 0.05f) /
            (minOf(primary.luminance(), reference.luminance()) + 0.05f)
        if (contrast < 4.5f) primary = lerp(primary, if (dark) Color.White else Color.Black, 0.1f)
    }
    return scheme.copy(
        primary = primary, onPrimary = primary.onAccent(), onPrimaryContainer = primary,
        background = background, surfaceContainerLowest = background,
        onSurfaceVariant = if (options.highContrast) scheme.onSurface else scheme.onSurfaceVariant,
        outline = if (options.highContrast) scheme.onSurface.copy(alpha = 0.8f) else scheme.outline,
        outlineVariant = if (options.highContrast) scheme.onSurface.copy(alpha = 0.45f) else scheme.outlineVariant,
    )
}

@Composable
fun AnimeScheduleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    options: ThemeOptions = ThemeOptions(),
    content: @Composable () -> Unit,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val hour by produceState(initialValue = LocalTime.now().hour, options.scheduled) {
        if (options.scheduled) while (true) {
            value = LocalTime.now().hour
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val darkTheme = if (options.scheduled) options.isDarkAt(hour) else when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = remember(darkTheme, options) { buildAppColorScheme(darkTheme, options) }
    val systemMotion = rememberMotionPolicy()
    val motion = MotionPolicy(systemMotion.reduceMotion || options.reduceMotion)

    // enableEdgeToEdge() follows the system theme by default. Keep system-bar icon
    // contrast synchronized with the in-app theme, including forced Light/Dark modes.
    val view = LocalView.current
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        val window = activity.window
        // Only API 31-34 still honor these (35+ enforces edge-to-edge), so they remain necessary.
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        window.isNavigationBarContrastEnforced = false
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnimeScheduleShapes,
    ) {
        CompositionLocalProvider(
            LocalCompactLayout provides options.compactLayout,
            LocalAmoledDark provides (darkTheme && options.amoled),
        ) {
            ProvideMotionPolicy(policy = motion, content = content)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
