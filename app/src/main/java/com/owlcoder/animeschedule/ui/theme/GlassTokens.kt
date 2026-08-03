package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(8.dp),
    Medium(14.dp),
}

/** Thin optical edge and restrained translucency for navigation chrome only. */
object GlassTokens {
    val hairline: Dp = 0.5.dp
    val highlight = Color.White.copy(alpha = 0.28f)
    val darkHighlight = Color.White.copy(alpha = 0.10f)
    val shadow = Color.Black.copy(alpha = 0.14f)
    val neutralFillLight = Color.White.copy(alpha = 0.68f)
    val neutralFillDark = Color(0xFF28282C).copy(alpha = 0.58f)
    val interactiveFillLight = Color.White.copy(alpha = 0.46f)
    val interactiveFillDark = Color.White.copy(alpha = 0.11f)
    val maxBackdropBlurRadius: Dp = 16.dp
    val chromeRadius: Dp = 18.dp
    val sheetRadius: Dp = 26.dp
    val contentRadius: Dp = 13.dp
    val groupRadius: Dp = 12.dp
    val controlRadius: Dp = 10.dp
    val posterRadius: Dp = 9.dp
}
