package com.owlcoder.animeschedule.presentation.components

import android.app.ActivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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

/**
 * Achromatic Liquid Glass for floating navigation and interactive controls only.
 *
 * [blur] is an optical-depth token that controls shadow elevation. This component does not
 * perform real-time backdrop blur, which keeps scrolling and selection animations predictable.
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
    val shadowElevation = when (blur) {
        GlassBlur.None -> 0.dp
        GlassBlur.Soft -> 2.dp
        GlassBlur.Medium -> 5.dp
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                clip = false,
                ambientColor = GlassTokens.shadow,
                spotColor = GlassTokens.shadow,
            )
            .clip(shape)
            .background(palette.fill)
            .border(GlassTokens.hairline, palette.border, shape),
    ) {
        // BoxScope.matchParentSize participates only in placement, so optical layers never force
        // a wrap-content glass control to consume its parent's full constraints.
        if (backdrop != null) {
            Box(Modifier.matchParentSize()) { backdrop() }
            Box(
                Modifier
                    .matchParentSize()
                    .background(palette.backdrop),
            )
        }

        Box(
            Modifier
                .matchParentSize()
                .background(palette.ambient),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(palette.specular),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(palette.lowlight),
        )

        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
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
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val selected = tone == GlassTone.Accent

    val fill = when (tone) {
        GlassTone.Neutral -> if (dark) GlassTokens.neutralFillDark else GlassTokens.neutralFillLight
        GlassTone.Accent -> if (dark) GlassTokens.selectedFillDark else GlassTokens.selectedFillLight
        GlassTone.OnImage -> Color.Black.copy(alpha = 0.22f)
    }
    val border = when (tone) {
        GlassTone.OnImage -> Color.White.copy(alpha = 0.22f)
        else -> if (dark) {
            Color.White.copy(alpha = if (selected) 0.18f else 0.11f)
        } else {
            Color.Black.copy(alpha = if (selected) 0.12f else 0.075f)
        }
    }
    val topLight = when (tone) {
        GlassTone.OnImage -> Color.White.copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = if (dark) 0.11f else 0.58f)
    }
    val sideLight = Color.White.copy(alpha = if (dark) 0.035f else 0.17f)

    return GlassPalette(
        fill = fill,
        border = border,
        ambient = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to sideLight,
                0.48f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.03f else 0.012f),
            ),
        ),
        specular = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to topLight,
                0.10f to topLight.copy(alpha = topLight.alpha * 0.45f),
                0.34f to Color.Transparent,
            ),
        ),
        lowlight = Brush.verticalGradient(
            colorStops = arrayOf(
                0.74f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (dark) 0.08f else 0.045f),
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
