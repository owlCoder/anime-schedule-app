package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Accent is a semantic selected state only; the material itself remains achromatic. */
enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(18.dp),
}

/** Optical tokens shared by floating navigation and interactive Liquid Glass controls. */
object GlassTokens {
    val hairline: Dp = 0.5.dp

    val highlight = Color.White.copy(alpha = 0.20f)
    val darkHighlight = Color.White.copy(alpha = 0.08f)
    val lowlight = Color.Black.copy(alpha = 0.055f)
    val shadow = Color.Black.copy(alpha = 0.14f)

    // Regular glass stays lightweight. Selected glass is only slightly thicker, never opaque.
    val neutralFillLight = Color.White.copy(alpha = 0.46f)
    val neutralFillDark = Color.White.copy(alpha = 0.085f)
    val selectedFillLight = Color.White.copy(alpha = 0.62f)
    val selectedFillDark = Color.White.copy(alpha = 0.145f)
    val interactiveFillLight = Color.White.copy(alpha = 0.40f)
    val interactiveFillDark = Color.White.copy(alpha = 0.100f)

    val maxBackdropBlurRadius: Dp = 20.dp

    val chromeRadius: Dp = 25.dp
    val sheetRadius: Dp = 28.dp
    val contentRadius: Dp = 18.dp
    val groupRadius: Dp = 16.dp
    val controlRadius: Dp = 14.dp
    val posterRadius: Dp = 11.dp
}
