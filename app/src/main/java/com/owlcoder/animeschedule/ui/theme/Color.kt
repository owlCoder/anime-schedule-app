package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * iOS-like layer hierarchy. The page is grouped gray, content groups are true white,
 * and only controls use the secondary gray. Keeping these layers distinct prevents
 * the washed-out appearance visible when every surface shares nearly the same tone.
 */
val AppLightBackground = Color(0xFFF4F5FA)
val AppLightGrouped = Color(0xFFFFFFFF)
val AppLightElevated = Color(0xFFFBFCFF)
val AppLightSecondary = Color(0xFFE7EAF2)

// Dark hierarchy; AMOLED uses pure black page and sheet canvases, retaining contrast for cards.
val AppDarkBackground = Color(0xFF101116)
val AppDarkGrouped = Color(0xFF18191F)
val AppDarkElevated = Color(0xFF24252C)
val AppDarkSecondary = Color(0xFF2D2E36)
