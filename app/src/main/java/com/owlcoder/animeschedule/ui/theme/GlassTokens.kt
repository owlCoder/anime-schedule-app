package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Accent is a semantic selected state only; the material itself remains neutral. */
enum class GlassTone { Neutral, Accent, OnImage }

enum class GlassBlur(val radius: Dp) {
    None(0.dp),
    Soft(10.dp),
    Medium(18.dp),
}

/**
 * Optical tokens for the app-wide liquid material.
 *
 * Liquid glass is deliberately achromatic. Brand/accent color belongs to small semantic content
 * such as links and notification dots, never to the glass substrate itself.
 */
object GlassTokens {
    val hairline: Dp = 0.5.dp

    val highlight = Color.White.copy(alpha = 0.28f)
    val darkHighlight = Color.White.copy(alpha = 0.11f)
    val lowlight = Color.Black.copy(alpha = 0.08f)
    val shadow = Color.Black.copy(alpha = 0.20f)

    val neutralFillLight = Color.White.copy(alpha = 0.66f)
    val neutralFillDark = Color(0xFF242428).copy(alpha = 0.64f)
    val selectedFillLight = Color.White.copy(alpha = 0.82f)
    val selectedFillDark = Color.White.copy(alpha = 0.17f)
    val interactiveFillLight = Color.White.copy(alpha = 0.48f)
    val interactiveFillDark = Color.White.copy(alpha = 0.10f)

    val maxBackdropBlurRadius: Dp = 20.dp

    val chromeRadius: Dp = 28.dp
    val sheetRadius: Dp = 30.dp
    val contentRadius: Dp = 18.dp
    val groupRadius: Dp = 16.dp
    val controlRadius: Dp = 15.dp
    val posterRadius: Dp = 12.dp
}
