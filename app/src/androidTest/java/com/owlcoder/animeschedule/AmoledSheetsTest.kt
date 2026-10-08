package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.screens.settings.AppearanceSheet
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class AmoledSheetsTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun canvas(): Color {
        val pixels = compose.onNodeWithTag("sheet-canvas").captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    @Test fun overlayCanvasFollowsPalettesDynamicColorsAndScheduledMode() {
        var options by mutableStateOf(ThemeOptions())
        var mode by mutableStateOf(ThemeMode.DARK)
        var systemDark = false
        compose.setContent {
            val deviceDark = androidx.compose.foundation.isSystemInDarkTheme()
            SideEffect { systemDark = deviceDark }
            AnimeScheduleTheme(themeMode = mode, options = options) {
                AppSheet(onDismissRequest = {}, title = "Overlay") {
                    Box(Modifier.fillMaxWidth().height(80.dp).testTag("sheet-canvas"))
                    Box(Modifier.fillMaxWidth().height(48.dp)
                        .background(MaterialTheme.colorScheme.surface).testTag("sheet-card"))
                }
            }
        }
        for (palette in ThemePalette.entries) {
            compose.runOnIdle { options = options.copy(palette = palette, amoled = true) }
            assertEquals("AMOLED $palette", Color.Black, canvas())
            val card = compose.onNodeWithTag("sheet-card").captureToImage().toPixelMap()
            assertTrue("Card contrast $palette", card[card.width / 2, card.height / 2].luminance() > 0f)
            compose.runOnIdle { options = options.copy(amoled = false) }
            assertNotEquals("Normal dark $palette", Color.Black, canvas())
        }
        compose.runOnIdle { options = options.copy(amoled = true, dynamicColors = true) }
        assertEquals(Color.Black, canvas())
        compose.runOnIdle { options = options.copy(amoled = false) }
        assertNotEquals(Color.Black, canvas())
        compose.runOnIdle { mode = ThemeMode.LIGHT; options = options.copy(amoled = true) }
        assertTrue("Dynamic light stays light", canvas().luminance() > .8f)
        compose.runOnIdle { mode = ThemeMode.SYSTEM }
        if (systemDark) assertEquals("System dark", Color.Black, canvas())
        else assertTrue("System light", canvas().luminance() > .8f)

        val hour = LocalTime.now().hour
        compose.runOnIdle {
            mode = ThemeMode.LIGHT
            options = options.copy(dynamicColors = false, scheduled = true,
                darkStartHour = hour, darkEndHour = (hour + 1) % 24)
        }
        assertEquals("Scheduled dark overrides forced light", Color.Black, canvas())
        compose.runOnIdle {
            mode = ThemeMode.DARK
            options = options.copy(darkStartHour = (hour + 1) % 24, darkEndHour = hour)
        }
        assertTrue("Scheduled light overrides forced dark", canvas().luminance() > .8f)
    }

    @Test fun appearanceToggleRecolorsTheOpenOverlayAndLightModeStaysLight() {
        var options by mutableStateOf(ThemeOptions(palette = ThemePalette.FOREST, amoled = false))
        var mode by mutableStateOf(ThemeMode.DARK)
        compose.setContent {
            AnimeScheduleTheme(themeMode = mode, options = options) {
                AppearanceSheet(mode, AccentColor.GREEN, options, { mode = it },
                    { options = it }, {}, {})
            }
        }
        fun gutter(): Color {
            val pixels = compose.onNode(isDialog()).captureToImage().toPixelMap()
            return pixels[(pixels.width * .02f).toInt(), (pixels.height * .75f).toInt()]
        }
        val amoled = instrumentation.targetContext.getString(R.string.theme_amoled)
        compose.onNodeWithText(amoled).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(options.amoled) }
        assertEquals(Color.Black, gutter())
        screenshot("appearance-amoled")
        compose.onNodeWithText(amoled).performClick()
        assertNotEquals(Color.Black, gutter())
        screenshot("appearance-normal-dark")
        compose.onNodeWithText(amoled).performClick()
        compose.runOnIdle { mode = ThemeMode.LIGHT }
        assertTrue("Light overlay keeps a light canvas", gutter().luminance() > .8f)
        screenshot("appearance-light-amoled-enabled")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5101-$name.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
