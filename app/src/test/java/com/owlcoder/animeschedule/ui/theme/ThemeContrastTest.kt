package com.owlcoder.animeschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.owlcoder.animeschedule.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color) = (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
    @Test fun `all palettes and accents keep primary text and buttons readable in both modes`() {
        for (dark in listOf(false, true)) for (palette in ThemePalette.entries) for (accent in AccentColor.entries) {
            val colors = buildAppColorScheme(dark, accent, ThemeOptions(palette = palette))
            for (surface in listOf(colors.surface, colors.surfaceContainerHigh, colors.surfaceContainerHighest, colors.primaryContainer)) {
                assertTrue("$dark $palette $accent primary text", contrast(colors.primary, surface) >= 4.49f)
            }
            assertTrue("$dark $palette $accent button", contrast(colors.primary, colors.onPrimary) >= 4.49f)
        }
    }
    @Test fun `AMOLED affects dark background and contrast elevates secondary labels`() {
        val options = ThemeOptions(palette = ThemePalette.FOREST, amoled = true, highContrast = true)
        val dark = buildAppColorScheme(true, AccentColor.GREEN, options)
        val light = buildAppColorScheme(false, AccentColor.GREEN, options)
        assertEquals(Color.Black, dark.background)
        assertNotEquals(Color.Black, light.background)
        assertEquals(dark.onSurface, dark.onSurfaceVariant)
        assertNotEquals(Color.Black, buildAppColorScheme(true, AccentColor.GREEN, options.copy(amoled = false)).background)
    }
}
