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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone
import com.owlcoder.animeschedule.ui.theme.GlassTokens

/** Achromatic Liquid Glass for floating navigation and interactive controls only. */
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
            .background(palette.ambient)
            .background(palette.specular)
            .background(palette.lowlight)
            .border(GlassTokens.hairline, palette.border, shape),
    ) {
        if (backdrop != null) {
            Box(Modifier.fillMaxSize()) { backdrop() }
            Box(Modifier.fillMaxSize().background(palette.backdrop))
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
    val ambient: Brush,
    val specular: Brush,
    val lowlight: Brush,
    val backdrop: Brush,
)

@Composable
private fun glassPalette(tone: GlassTone): GlassPalette {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.35f

    val fill = when (tone) {
        GlassTone.Neutral -> if (dark) GlassTokens.neutralFillDark else GlassTokens.neutralFillLight
        GlassTone.Accent -> if (dark) GlassTokens.selectedFillDark else GlassTokens.selectedFillLight
        GlassTone.OnImage -> Color.Black.copy(alpha = 0.20f)
    }
    val border = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.16f else 0.46f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.085f else 0.34f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.17f)
    }
    val topLight = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.13f else 0.25f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.065f else 0.17f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.13f)
    }
    val sideLight = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.045f else 0.08f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.022f else 0.05f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.035f)
    }

    return GlassPalette(
        fill = fill,
        border = border,
        ambient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to sideLight,
                0.46f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.012f else 0.006f),
            ),
        ),
        specular = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to topLight,
                0.12f to topLight.copy(alpha = topLight.alpha * 0.36f),
                0.36f to Color.Transparent,
            ),
        ),
        lowlight = Brush.verticalGradient(
            colorStops = arrayOf(
                0.72f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.045f else 0.012f),
            ),
        ),
        backdrop = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.014f), Color.Transparent),
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
