package com.owlcoder.animeschedule.presentation.components

import android.app.ActivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone

enum class AppMaterial { Background, Grouped, Elevated, Interactive }

@Composable
fun appMaterialColor(material: AppMaterial): Color {
    val colors = MaterialTheme.colorScheme
    return when (material) {
        AppMaterial.Background -> colors.background
        AppMaterial.Grouped -> colors.surfaceContainerLow
        AppMaterial.Elevated -> colors.surfaceContainerHigh
        AppMaterial.Interactive -> colors.primaryContainer
    }
}

/** Neutral grouped/elevated surface. Use [GlassSurface] for chrome and focused actions. */
@Composable
fun AppMaterialSurface(
    modifier: Modifier = Modifier,
    material: AppMaterial = AppMaterial.Grouped,
    shape: Shape = ContinuousRoundedShape(16.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = appMaterialColor(material),
        contentColor = contentColor,
        tonalElevation = 0.dp,
        content = content,
    )
}

/**
 * Compatibility host for older call sites. Modifier.blur() is intentionally not used here:
 * it blurs the host's own layer, not the content behind it. GlassSurface supplies the
 * deterministic tint/highlight fallback until a sampled backdrop host is introduced.
 */
@Composable
fun MaterialBackdropHost(
    modifier: Modifier = Modifier,
    blur: GlassBlur = GlassBlur.None,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) { content() }
}

/** Shared neutral scrim for full-height surfaces and launch transitions. */
@Composable
fun AppScrim(modifier: Modifier = Modifier, alpha: Float = 0.20f) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
    )
}
