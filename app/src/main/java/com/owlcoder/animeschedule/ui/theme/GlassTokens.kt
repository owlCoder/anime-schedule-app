package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(16.dp),
}

/** Restrained glass vocabulary: thin optical edge, low shadow and compact radii. */
object GlassTokens {
    val hairline: Dp = 0.5.dp
    val highlight = Color.White.copy(alpha = 0.34f)
    val darkHighlight = Color.White.copy(alpha = 0.12f)
    val shadow = Color.Black.copy(alpha = 0.18f)
    val neutralFillLight = Color.White.copy(alpha = 0.72f)
    val neutralFillDark = Color(0xFF242428).copy(alpha = 0.66f)
    val interactiveFillLight = Color.White.copy(alpha = 0.50f)
    val interactiveFillDark = Color.White.copy(alpha = 0.13f)
    val maxBackdropBlurRadius: Dp = 18.dp
    val chromeRadius: Dp = 20.dp
    val sheetRadius: Dp = 28.dp
    val contentRadius: Dp = 14.dp
    val groupRadius: Dp = 14.dp
    val controlRadius: Dp = 11.dp
    val posterRadius: Dp = 10.dp
}
