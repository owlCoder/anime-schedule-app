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

/**
 * Achromatic liquid material for floating navigation, controls and transient overlays.
 *
 * Compose cannot sample the UIKit backdrop pipeline directly, so this primitive builds depth from
 * translucent neutral fill, edge light, a soft specular band and a lowlight. Accent color is never
 * baked into the glass itself; selected glass is simply a brighter neutral lens.
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
    val elevation = when (blur) {
        GlassBlur.None -> 0.dp
        GlassBlur.Soft -> 2.dp
        GlassBlur.Medium -> 4.dp
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
    val ambient: Brush,
    val specular: Brush,
    val lowlight: Brush,
    val backdrop: Brush,
)

@Composable
private fun glassPalette(tone: GlassTone): GlassPalette {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.red < 0.2f

    val fill = when (tone) {
        GlassTone.Neutral -> if (dark) GlassTokens.neutralFillDark else GlassTokens.neutralFillLight
        GlassTone.Accent -> if (dark) GlassTokens.selectedFillDark else GlassTokens.selectedFillLight
        GlassTone.OnImage -> Color.Black.copy(alpha = 0.26f)
    }

    val border = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.20f else 0.78f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.12f else 0.66f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.20f)
    }

    val ambientTop = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.075f else 0.16f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.065f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.04f else 0.10f)
    }

    val specularTop = when (tone) {
        GlassTone.Accent -> Color.White.copy(alpha = if (dark) 0.17f else 0.38f)
        GlassTone.OnImage -> Color.White.copy(alpha = 0.17f)
        GlassTone.Neutral -> Color.White.copy(alpha = if (dark) 0.095f else 0.27f)
    }

    return GlassPalette(
        fill = fill,
        border = border,
        ambient = Brush.horizontalGradient(
            colors = listOf(
                ambientTop,
                Color.Transparent,
                ambientTop.copy(alpha = ambientTop.alpha * 0.26f),
            ),
        ),
        specular = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to specularTop,
                0.16f to specularTop.copy(alpha = specularTop.alpha * 0.32f),
                0.52f to Color.Transparent,
            ),
        ),
        lowlight = Brush.verticalGradient(
            colorStops = arrayOf(
                0.62f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.085f else 0.025f),
            ),
        ),
        backdrop = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.025f), Color.Transparent),
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
