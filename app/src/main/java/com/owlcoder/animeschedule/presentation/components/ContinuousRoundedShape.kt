package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A stable, Compose-native continuous-corner vocabulary.
 *
 * Android's public Compose API does not expose Apple's private continuous corner
 * primitive. This shape intentionally keeps the same soft visual language while
 * delegating outline creation to the platform-optimized rounded shape.
 */
data class ContinuousRoundedShape(val corner: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = RoundedCornerShape(corner).createOutline(size, layoutDirection, density)
}
