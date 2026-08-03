package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(18.dp),
}

/**
 * Optical tokens for the iOS-inspired material system.
 *
 * Glass is reserved for floating navigation, controls and transient overlays. Content uses
 * quiet grouped surfaces so the chrome remains visually distinct.
 */
object GlassTokens {
    val hairline: Dp = 0.6.dp

    val highlight = Color.White.copy(alpha = 0.34f)
    val darkHighlight = Color.White.copy(alpha = 0.13f)
    val lowlight = Color.Black.copy(alpha = 0.10f)
    val shadow = Color.Black.copy(alpha = 0.24f)

    val neutralFillLight = Color.White.copy(alpha = 0.74f)
    val neutralFillDark = Color(0xFF29292D).copy(alpha = 0.72f)
    val interactiveFillLight = Color.White.copy(alpha = 0.52f)
    val interactiveFillDark = Color.White.copy(alpha = 0.13f)

    val maxBackdropBlurRadius: Dp = 20.dp

    val chromeRadius: Dp = 28.dp
    val sheetRadius: Dp = 32.dp
    val contentRadius: Dp = 20.dp
    val groupRadius: Dp = 18.dp
    val controlRadius: Dp = 16.dp
    val posterRadius: Dp = 12.dp
}
