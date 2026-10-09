package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.locale.ProvideAppLocale
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.screens.onboarding.OnboardingScreen
import com.owlcoder.animeschedule.presentation.screens.settings.AppearanceSheet
import com.owlcoder.animeschedule.presentation.screens.settings.SettingsIconTile
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Appearance5127UiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun compactAppearanceHasExactlyFifteenSelectablePalettesAndNoLegacySections() {
        var options by mutableStateOf(ThemeOptions())
        var mode by mutableStateOf(ThemeMode.LIGHT)
        compose.setContent { ProvideAppLocale(AppLanguage.ENGLISH) {
            AnimeScheduleTheme(themeMode = mode, options = options) {
                AppearanceSheet(mode, options, { mode = it }, { options = options.copy(palette = it) }, {}, {})
            }
        } }
        assertEquals(15, ThemePalette.entries.size)
        compose.onNodeWithTag("appearance-content").assertHeightIsAtLeast(48.dp)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("Display").assertDoesNotExist()
        compose.onNodeWithTag("preset-name").assertDoesNotExist()
        compose.onNodeWithTag("theme-accent-GREEN").assertDoesNotExist()
        for (palette in ThemePalette.entries) {
            compose.onNodeWithTag("theme-palette-${palette.name}").performScrollTo()
                .assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
            compose.runOnIdle { assertEquals(palette, options.palette) }
        }
        compose.onNodeWithTag("theme-mode-picker").performScrollTo()
        compose.onNodeWithText("Dark").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, mode) }
        screenshot("appearance-compact-dark")
    }

    @Test fun largeSerbianAppearanceKeepsLabelsAndModeControlsReachable() {
        var options by mutableStateOf(ThemeOptions(palette = ThemePalette.OCEAN))
        compose.setContent { ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
                AnimeScheduleTheme(themeMode = ThemeMode.LIGHT, options = options) {
                    AppearanceSheet(ThemeMode.LIGHT, options, {}, { options = options.copy(palette = it) }, {}, {})
                }
            }
        } }
        compose.onNodeWithText("Sistem").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("theme-palette-AMETHYST").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Ametist").assertIsDisplayed()
        screenshot("appearance-serbian-large-light")
        compose.onNodeWithTag("theme-palette-GOLD").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemePalette.GOLD, options.palette) }
    }

    @Test fun onboardingUsesTheSamePalettesAndKeepsFinishVisibleWithLargeText() {
        var palette by mutableStateOf(ThemePalette.CLASSIC)
        var mode by mutableStateOf(ThemeMode.LIGHT)
        var completed = false
        compose.setContent { ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
                AnimeScheduleTheme(themeMode = mode, options = ThemeOptions(palette = palette)) {
                    OnboardingScreen({ completed = true }, {}, false, "", mode, palette,
                        AppLanguage.SERBIAN_LATIN, { mode = it }, { palette = it }, {})
                }
            }
        } }
        repeat(5) { compose.onNodeWithText("Nastavi").assertIsDisplayed().performClick(); compose.waitForIdle() }
        compose.onNodeWithTag("onboarding-theme-page").assertIsDisplayed()
        compose.onNodeWithTag("theme-palette-MINT").performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ThemePalette.MINT, palette) }
        compose.onNodeWithText("Započni").assertIsDisplayed()
        screenshot("onboarding-palettes-serbian-large")
        compose.onNodeWithText("Započni").performClick()
        compose.runOnIdle { assertTrue(completed) }
    }

    @Test fun settingsIconTilesFollowPaletteAndLightDarkChanges() {
        var palette by mutableStateOf(ThemePalette.FOREST)
        var mode by mutableStateOf(ThemeMode.LIGHT)
        var expected = Color.Unspecified
        compose.setContent {
            AnimeScheduleTheme(themeMode = mode, options = ThemeOptions(palette = palette)) {
                val primary = MaterialTheme.colorScheme.primary
                SideEffect { expected = primary }
                SettingsIconTile(Icons.Default.Palette, modifier = Modifier.testTag("settings-icon"))
            }
        }
        val samples = mutableSetOf<Color>()
        for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) for (color in listOf(ThemePalette.FOREST, ThemePalette.RUBY, ThemePalette.GOLD)) {
            compose.runOnIdle { mode = theme; palette = color }
            val pixels = compose.onNodeWithTag("settings-icon").captureToImage().toPixelMap()
            val sample = pixels[(pixels.width * .15f).toInt(), pixels.height / 2]
            assertEquals(expected.red, sample.red, .01f); assertEquals(expected.green, sample.green, .01f); assertEquals(expected.blue, sample.blue, .01f)
            samples += sample
        }
        assertEquals(6, samples.size)
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5127-$name.png").outputStream()
            .use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
