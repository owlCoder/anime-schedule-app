package com.owlcoder.animeschedule.presentation.components

import android.app.ActivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.GlassTokens

/**
 * Selective glass treatment for chrome, controls and transient surfaces.
 *
 * This is an honest tonal glass fallback: Compose's Modifier.blur() blurs the layer it is
 * attached to, not the content behind it. A real sampled backdrop belongs in a dedicated
 * host and is intentionally not faked here. Dimensions and hierarchy remain identical on
 * every device while the tint, highlight and hairline communicate translucency.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousRoundedShape(GlassTokens.contentRadius),
    tone: GlassTone = GlassTone.Neutral,
    blur: GlassBlur = GlassBlur.None,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backdrop: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = glassPalette(tone)
    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.fill)
            .background(palette.highlight)
            .border(GlassTokens.hairline, palette.border, shape),
    ) {
        // Kept as an optional compatibility hook. It is tonal decoration, never a claim of
        // backdrop sampling and never part of the foreground layout measurement.
        if (backdrop != null) {
            Box(Modifier.fillMaxSize().background(palette.backdrop)) { backdrop() }
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

/** Named semantic primitive for top bars, floating controls and bottom navigation. */
@Composable
fun GlassChrome(
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousRoundedShape(GlassTokens.chromeRadius),
    tone: GlassTone = GlassTone.Neutral,
    content: @Composable () -> Unit,
) {
    GlassSurface(modifier = modifier, shape = shape, tone = tone, blur = GlassBlur.Medium, content = content)
}

private data class GlassPalette(
    val fill: Color,
    val border: Color,
    val highlight: Brush,
    val backdrop: Brush,
)

@Composable
private fun glassPalette(tone: GlassTone): GlassPalette {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.red < 0.2f
    val accent = colors.primary
    val fill = when (tone) {
        GlassTone.Neutral -> if (dark) GlassTokens.neutralFillDark else GlassTokens.neutralFillLight
        GlassTone.Accent -> accent.copy(alpha = if (dark) 0.22f else 0.16f)
        GlassTone.OnImage -> Color.Black.copy(alpha = 0.22f)
    }
    val border = if (dark) GlassTokens.darkHighlight else GlassTokens.highlight
    val top = when (tone) {
        GlassTone.Accent -> accent.copy(alpha = if (dark) 0.13f else 0.18f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.16f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.08f else 0.34f)
    }
    return GlassPalette(
        fill = fill,
        border = border,
        highlight = Brush.verticalGradient(listOf(top, Color.Transparent)),
        backdrop = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)),
    )
}

/** Retained for callers that need a low-RAM/API capability check before adding decoration. */
@Composable
fun rememberGlassCapability(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice != true
    }
}
