package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Accent is a semantic selected state; glass itself stays achromatic. */
enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(18.dp),
}

object GlassTokens {
    val hairline: Dp = 0.5.dp

    val shadow = Color.Black.copy(alpha = 0.16f)
    val neutralFillLight = Color.White.copy(alpha = 0.72f)
    val neutralFillDark = Color(0xFF2C2C2E).copy(alpha = 0.56f)
    val selectedFillLight = Color.White.copy(alpha = 0.82f)
    val selectedFillDark = Color(0xFF3A3A3C).copy(alpha = 0.64f)
    val interactiveFillLight = Color.White.copy(alpha = 0.78f)
    val interactiveFillDark = Color(0xFF2C2C2E).copy(alpha = 0.68f)

    val maxBackdropBlurRadius: Dp = 20.dp

    val chromeRadius: Dp = 24.dp
    val sheetRadius: Dp = 24.dp
    val contentRadius: Dp = 16.dp
    val groupRadius: Dp = 14.dp
    val controlRadius: Dp = 12.dp
    val posterRadius: Dp = 10.dp
}
