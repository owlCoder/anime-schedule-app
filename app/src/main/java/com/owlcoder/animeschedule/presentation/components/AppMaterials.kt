package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.owlcoder.animeschedule.ui.theme.GlassTokens
import androidx.compose.ui.unit.dp

enum class AppMaterial { Background, Grouped, Elevated, Interactive }

@Composable
fun appMaterialColor(material: AppMaterial): Color {
    return when (material) {
        AppMaterial.Background -> MaterialTheme.colorScheme.background
        AppMaterial.Grouped -> MaterialTheme.colorScheme.surface
        AppMaterial.Elevated -> MaterialTheme.colorScheme.surfaceContainerHigh
        AppMaterial.Interactive -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
}

/** Stable content material. Liquid Glass is reserved for floating chrome and controls. */
@Composable
fun AppMaterialSurface(
    modifier: Modifier = Modifier,
    material: AppMaterial = AppMaterial.Grouped,
    shape: Shape = ContinuousRoundedShape(GlassTokens.contentRadius),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val border = if (material == AppMaterial.Background) null else BorderStroke(
        0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
    )
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
