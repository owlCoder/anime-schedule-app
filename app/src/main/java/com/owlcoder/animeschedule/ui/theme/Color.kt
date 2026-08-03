package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy aliases retained for existing screens. The visual system below is deliberately
// semantic and does not use Android dynamic colors for its neutral surfaces.
val TgBlue = Color(0xFF007AFF)
val TgBlueDark = Color(0xFF0A84FF)
val TgSurface = Color(0xFFFFFFFF)
val TgBackground = Color(0xFFF2F2F7)
val TgCard = Color(0xFFFFFFFF)
val TgDivider = Color(0x2E3C3C43)

// Dark surfaces are layered rather than pure black, which keeps hierarchy visible.
val TgBlueDarkMode = Color(0xFF0A84FF)
val TgSurfaceDark = Color(0xFF1C1C1E)
val TgBackgroundDark = Color(0xFF000000)
val TgCardDark = Color(0xFF2C2C2E)

// Legacy (keep for Material fallback)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/** Neutral iOS-inspired surfaces. Accent is intentionally kept out of grouped materials. */
val AppLightBackground = Color(0xFFF2F2F7)
val AppLightGrouped = Color(0xFFFFFFFF)
val AppLightElevated = Color(0xFFFFFFFF)
val AppLightSecondary = Color(0xFFE5E5EA)
val AppDarkBackground = Color(0xFF000000)
val AppDarkGrouped = Color(0xFF1C1C1E)
val AppDarkElevated = Color(0xFF2C2C2E)
val AppDarkSecondary = Color(0xFF3A3A3C)
