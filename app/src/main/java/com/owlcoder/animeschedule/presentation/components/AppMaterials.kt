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
import com.owlcoder.animeschedule.ui.theme.AppDarkElevated
import com.owlcoder.animeschedule.ui.theme.AppDarkGrouped
import com.owlcoder.animeschedule.ui.theme.AppDarkSecondary
import com.owlcoder.animeschedule.ui.theme.AppLightElevated
import com.owlcoder.animeschedule.ui.theme.AppLightGrouped
import com.owlcoder.animeschedule.ui.theme.AppLightSecondary
import com.owlcoder.animeschedule.ui.theme.GlassBlur

enum class AppMaterial { Background, Grouped, Elevated, Interactive }

@Composable
fun appMaterialColor(material: AppMaterial): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.35f
    return when (material) {
        AppMaterial.Background -> MaterialTheme.colorScheme.background
        AppMaterial.Grouped -> if (dark) AppDarkGrouped else AppLightGrouped
        AppMaterial.Elevated -> if (dark) AppDarkElevated else AppLightElevated
        AppMaterial.Interactive -> if (dark) AppDarkSecondary else AppLightSecondary
    }
}

/** Stable content material. Liquid Glass is reserved for floating chrome and controls. */
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
            if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
        )
        AppMaterial.Elevated -> BorderStroke(
            0.5.dp,
            if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f),
        )
        AppMaterial.Interactive -> null
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
fun AppScrim(modifier: Modifier = Modifier, alpha: Float = 0.24f) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha)),
    )
}
