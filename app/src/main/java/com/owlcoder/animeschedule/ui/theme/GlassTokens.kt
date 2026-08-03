package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small, deliberate glass vocabulary. Keep blur opt-in and out of scrolling lists. */
enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(16.dp),
}

/** Fallback-friendly glass constants shared by surfaces, buttons, and loading UI. */
object GlassTokens {
    val hairline: Dp = 1.dp
    val highlight = Color.White.copy(alpha = 0.42f)
    val darkHighlight = Color.White.copy(alpha = 0.16f)
    val shadow = Color.Black.copy(alpha = 0.24f)
    val neutralFillLight = Color.White.copy(alpha = 0.66f)
    val neutralFillDark = Color(0xFF1C1C1E).copy(alpha = 0.78f)
    val interactiveFillLight = Color.White.copy(alpha = 0.52f)
    val interactiveFillDark = Color.White.copy(alpha = 0.16f)
    val maxBackdropBlurRadius: Dp = 18.dp
    val chromeRadius: Dp = 22.dp
    val sheetRadius: Dp = 28.dp
    val contentRadius: Dp = 16.dp
    val groupRadius: Dp = 14.dp
    val controlRadius: Dp = 12.dp
    val posterRadius: Dp = 10.dp
}
