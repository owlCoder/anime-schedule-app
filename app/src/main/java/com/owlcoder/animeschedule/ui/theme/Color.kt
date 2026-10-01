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

// AMOLED-first iOS hierarchy: true black canvas with barely lifted neutral surfaces.
val AppDarkBackground = Color(0xFF000000)
val AppDarkGrouped = Color(0xFF09090A)
val AppDarkElevated = Color(0xFF0F0F11)
val AppDarkSecondary = Color(0xFF171719)
