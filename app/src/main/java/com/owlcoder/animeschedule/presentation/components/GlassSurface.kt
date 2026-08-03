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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.GlassTokens

/** Translucent material reserved for navigation, transient controls and overlays. */
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
    val elevation = when (blur) {
        GlassBlur.None -> 0.dp
        GlassBlur.Soft -> 1.dp
        GlassBlur.Medium -> 2.dp
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = GlassTokens.shadow,
                spotColor = GlassTokens.shadow,
            )
            .clip(shape)
            .background(palette.fill)
            .background(palette.highlight)
            .border(GlassTokens.hairline, palette.border, shape),
    ) {
        if (backdrop != null) {
            Box(Modifier.fillMaxSize().background(palette.backdrop)) { backdrop() }
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

@Composable
fun GlassChrome(
    modifier: Modifier = Modifier,
    shape: Shape = ContinuousRoundedShape(GlassTokens.chromeRadius),
    tone: GlassTone = GlassTone.Neutral,
    content: @Composable () -> Unit,
) = GlassSurface(
    modifier = modifier,
    shape = shape,
    tone = tone,
    blur = GlassBlur.Medium,
    content = content,
)

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
        GlassTone.Accent -> accent.copy(alpha = if (dark) 0.17f else 0.10f)
        GlassTone.OnImage -> Color.Black.copy(alpha = 0.22f)
    }
    val border = when (tone) {
        GlassTone.Accent -> accent.copy(alpha = if (dark) 0.30f else 0.22f)
        GlassTone.Neutral -> if (dark) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.54f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.16f)
    }
    val top = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.055f else 0.22f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.12f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.045f else 0.24f)
    }
    return GlassPalette(
        fill = fill,
        border = border,
        highlight = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to top,
                0.38f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.035f else 0f),
            ),
        ),
        backdrop = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.025f), Color.Transparent),
        ),
    )
}

@Composable
fun rememberGlassCapability(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice != true
    }
}
