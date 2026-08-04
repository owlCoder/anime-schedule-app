package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.ui.theme.GlassBlur

enum class AppMaterial { Background, Grouped, Elevated, Interactive }

@Composable
fun appMaterialColor(material: AppMaterial): Color {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.35f
    return when (material) {
        AppMaterial.Background -> colors.background
        AppMaterial.Grouped -> if (dark) Color.White.copy(alpha = 0.075f) else Color.Black.copy(alpha = 0.040f)
        AppMaterial.Elevated -> if (dark) Color.White.copy(alpha = 0.105f) else Color.White.copy(alpha = 0.90f)
        AppMaterial.Interactive -> if (dark) Color.White.copy(alpha = 0.125f) else Color.Black.copy(alpha = 0.055f)
    }
}

/**
 * Quiet content material. It intentionally stays lighter than chrome and never competes with
 * floating liquid-glass controls. A very soft hairline keeps grouped content readable without
 * turning every section into a heavy grey card.
 */
@Composable
fun AppMaterialSurface(
    modifier: Modifier = Modifier,
    material: AppMaterial = AppMaterial.Grouped,
    shape: Shape = ContinuousRoundedShape(16.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    val border = when (material) {
        AppMaterial.Background -> null
        AppMaterial.Grouped -> BorderStroke(
            0.5.dp,
            if (dark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.055f),
        )
        AppMaterial.Elevated, AppMaterial.Interactive -> BorderStroke(
            0.5.dp,
            if (dark) Color.White.copy(alpha = 0.085f) else Color.Black.copy(alpha = 0.065f),
        )
    }
    Surface(
        modifier = modifier,
        shape = shape,
        color = appMaterialColor(material),
        contentColor = contentColor,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

@Composable
fun MaterialBackdropHost(
    modifier: Modifier = Modifier,
    blur: GlassBlur = GlassBlur.None,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) { content() }
}

@Composable
fun AppScrim(modifier: Modifier = Modifier, alpha: Float = 0.20f) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
    )
}
