package com.owlcoder.animeschedule

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.settings.AppearanceSheet
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Review screenshots stress translation, long titles, large text and the real on-screen IME. */
class VisualReviewTest {
    @get:Rule
    val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Composable
    private fun SerbianLargeText(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val density = LocalDensity.current
        val baseConfiguration = LocalConfiguration.current
        val config = remember(baseConfiguration) {
            Configuration(baseConfiguration).apply {
                setLocale(
                    Locale.forLanguageTag("sr-Latn")
                )
            }
        }
        val localized = remember(context, config) { context.createConfigurationContext(config) }
        CompositionLocalProvider(
            LocalContext provides localized,
            LocalConfiguration provides config,
            LocalResources provides localized.resources,
            LocalDensity provides Density(density.density, 1.35f),
            content = content
        )
    }

    @Test
    fun longTitleLargeProgressAndNotesRemainUsableWithKeyboardInSerbian() {
        val entry = MalListEntry(
            6001,
            "Sora wa Akai Kawa no Hotori — veoma dugačak naslov animea za proveru rasporeda",
            status = WatchStatus.WATCHING,
            episodesWatched = 42,
            score = 0,
            totalEpisodes = 10000
        )
        var saved: MalListUpdate? = null
        compose.setContent {
            SerbianLargeText {
                AnimeScheduleTheme(
                    themeMode = ThemeMode.LIGHT,
                    options = ThemeOptions(palette = ThemePalette.SAKURA)
                ) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    ListStatusBottomSheet(6001, entry, {}, { _, update -> saved = update })
                }
            }
        }
        compose.onNodeWithTag("list-editor-episodes").performScrollTo()
            .performTextReplacement("99999")
        compose.onNodeWithTag("list-editor-episodes").performImeAction()
        compose.onNodeWithTag("list-editor-episodes").assertTextContains("10000")
        screenshot("editor-serbian-large-progress")
        val noteLabel =
            instrumentation.targetContext.createConfigurationContext(Configuration(instrumentation.targetContext.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("sr-Latn"))
                fontScale = 1.35f
            }).getString(R.string.personal_note)
        compose.onNodeWithText(noteLabel).performScrollTo().performClick()
        compose.onNodeWithTag("editor-note").performScrollTo()
            .performClick()
            .performTextInput("Nastaviti sledeće nedelje, proveriti završnu scenu.")
        compose.onNodeWithTag("editor-note").assertIsDisplayed()
        compose.onNodeWithTag("list-editor-save").assertIsDisplayed()
        screenshot("editor-serbian-large-keyboard")
        compose.onNodeWithTag("list-editor-save").performClick()
        compose.runOnIdle { assertEquals(10000, saved?.episodesWatched) }
    }

    @Test
    fun themeIconsAndEveryPaletteAreReadableInSerbianWithLargerText() {
        var options by mutableStateOf(ThemeOptions(palette = ThemePalette.FOREST, amoled = false))
        var appearanceVisible by mutableStateOf(true)
        compose.setContent {
            SerbianLargeText {
                AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = options) {
                    AnimatedSplashScreen(Modifier.fillMaxSize())
                    if (appearanceVisible) AppearanceSheet(
                        ThemeMode.DARK,
                        options,
                        {},
                        { options = options.copy(palette = it) },
                        {},
                        { appearanceVisible = false })
                }
            }
        }
        compose.onNodeWithTag("theme-palette-SAND").performScrollTo().assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(ThemePalette.SAND, options.palette) }
        screenshot("appearance-serbian-large")
        val localized = instrumentation.targetContext.createConfigurationContext(
            Configuration(instrumentation.targetContext.resources.configuration).apply { setLocale(Locale.forLanguageTag("sr-Latn")) }
        )
        compose.onNodeWithContentDescription(localized.getString(android.R.string.cancel)).performClick()
        compose.onNodeWithTag("schedule-loading").assertIsDisplayed()
        screenshot("orbit-loading-serbian-large")
    }

    @Test
    fun floatingMessagesRemainReadableAndCanBeDismissed() {
        val controller = ToastController()
        compose.setContent {
            SerbianLargeText {
                AnimeScheduleTheme(
                    themeMode = ThemeMode.DARK,
                    options = ThemeOptions(palette = ThemePalette.FOREST)
                ) {
                    CompositionLocalProvider(LocalNavBarHeight provides 84.dp) {
                        ToastHost(controller) {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background).padding(24.dp)
                            ) {
                                Text(
                                    "Anime Schedule",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.runOnIdle { controller.success("Napredak gledanja je sačuvan") }
        compose.onNodeWithTag("app-toast").assertIsDisplayed()
        screenshot("toast-success-serbian")
        compose.onNodeWithTag("toast-dismiss").performClick()
        compose.runOnIdle { assertNull(controller.current); controller.error("Izmena nije sačuvana. Pokušaj ponovo kada se vrati internet veza.") }
        compose.onNodeWithTag("app-toast").assertIsDisplayed()
        screenshot("toast-error-serbian")
        compose.onNodeWithTag("toast-dismiss").performClick()
        compose.runOnIdle { assertNull(controller.current) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(
            instrumentation.targetContext.getExternalFilesDir(null),
            "qa-530-$name.png"
        ).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
