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

/** Optical tokens shared by every floating liquid-glass control. */
object GlassTokens {
    val hairline: Dp = 0.5.dp

    val highlight = Color.White.copy(alpha = 0.24f)
    val darkHighlight = Color.White.copy(alpha = 0.09f)
    val lowlight = Color.Black.copy(alpha = 0.065f)
    val shadow = Color.Black.copy(alpha = 0.15f)

    // Dark glass is deliberately translucent instead of a nearly-opaque charcoal card.
    val neutralFillLight = Color.White.copy(alpha = 0.68f)
    val neutralFillDark = Color.White.copy(alpha = 0.080f)
    val selectedFillLight = Color.White.copy(alpha = 0.90f)
    val selectedFillDark = Color.White.copy(alpha = 0.165f)
    val interactiveFillLight = Color.White.copy(alpha = 0.50f)
    val interactiveFillDark = Color.White.copy(alpha = 0.105f)

    val maxBackdropBlurRadius: Dp = 20.dp

    val chromeRadius: Dp = 26.dp
    val sheetRadius: Dp = 28.dp
    val contentRadius: Dp = 18.dp
    val groupRadius: Dp = 16.dp
    val controlRadius: Dp = 14.dp
    val posterRadius: Dp = 11.dp
}
