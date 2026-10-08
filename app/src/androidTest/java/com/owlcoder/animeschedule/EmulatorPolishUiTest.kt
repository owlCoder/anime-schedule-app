package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.locale.ProvideAppLocale
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.settings.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EmulatorPolishUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val models = ViewModelStore()
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    @After fun cleanup() { instrumentation.runOnMainSync { models.clear() } }

    private class Sources : WatchSourceRepository {
        val rows = MutableStateFlow(listOf(
            WatchSource(1, "Alpha source", "https://example.com/?q={query}", null),
            WatchSource(2, "Beta source", "https://example.org/?q={query}", null),
        ))
        override fun getAll() = rows
        override suspend fun add(name: String, urlTemplate: String, faviconUrl: String?, openExternally: Boolean) = Unit
        override suspend fun update(source: WatchSource) { rows.value = rows.value.map { if (it.id == source.id) source else it } }
        override suspend fun delete(source: WatchSource) { rows.value = rows.value.filterNot { it.id == source.id } }
    }

    @Test fun systemBackReturnsFromSourceEditorThenClosesAndCanReopen() {
        val sources = Sources()
        val vm = WatchSourcesViewModel(sources).also { models.put("sources", it) }
        var visible by mutableStateOf(true)
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK) {
            SheetBackdropHost(Modifier.fillMaxSize()) {
                AppButton("Open sources", { visible = true })
                if (visible) WatchSourcesBottomSheet({ visible = false }, vm)
            }
        } }
        compose.onNodeWithText("Alpha source").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Unsaved edit")
        // Text replacement focuses the field and opens the IME. Hide it before testing
        // modal navigation; the first Back with an IME is correctly consumed by Android.
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("sources-after-editor-back")
        compose.onNodeWithText("Alpha source").assertIsDisplayed()
        compose.onNodeWithText("Beta source").performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Beta source")
        Espresso.pressBack()
        compose.onNodeWithText("Beta source").assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.watch_sources_add)).performClick()
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("")
        Espresso.pressBack()
        compose.onNodeWithText("Alpha source").assertIsDisplayed()
        Espresso.pressBack()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.runOnIdle { assertFalse(visible); assertEquals("Alpha source", sources.rows.value.first().name) }
        compose.onNodeWithText("Open sources").performClick()
        compose.onNodeWithText("Alpha source").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Composable private fun SerbianLarge(content: @Composable () -> Unit) {
        ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f), content = content)
        }
    }

    @Test fun sourceEditorSupportsNextDoneAndSavingAtLargeSerbianText() {
        val sources = Sources()
        val vm = WatchSourcesViewModel(sources).also { models.put("sources", it) }
        compose.setContent { SerbianLarge {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(palette = ThemePalette.ICE, amoled = true)) {
                WatchSourcesBottomSheet({}, vm)
            }
        } }
        compose.onNodeWithText("Alpha source").performClick()
        compose.onNodeWithTag("source-name-container").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("source-name").performClick().performTextReplacement("Updated Alpha")
        compose.onNodeWithTag("source-name").performImeAction()
        compose.onNodeWithTag("source-url").assertIsFocused().performTextReplacement("https://example.com/?q={query}")
        compose.onNodeWithText("Sačuvaj").assertIsDisplayed()
        screenshot("source-keyboard-large-dark")
        compose.onNodeWithTag("source-url").performImeAction()
        compose.onNodeWithTag("source-url").assertIsNotFocused()
        screenshot("source-large-dark")
        compose.onNodeWithText("Sačuvaj").performClick()
        compose.waitUntil { sources.rows.value.first().name == "Updated Alpha" }
        compose.onNodeWithText("Updated Alpha").assertIsDisplayed()
        compose.onNodeWithText("Beta source").performClick()
        compose.onNodeWithTag("source-name").assertTextContains("Beta source")
    }

    @Test fun appearanceUsesOneAccessibleToggleWithAnAmoledThumb() {
        var options by mutableStateOf(ThemeOptions(palette = ThemePalette.ICE, amoled = true))
        compose.setContent { SerbianLarge {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = options) {
                AppearanceSheet(ThemeMode.DARK, AccentColor.TELEGRAM_BLUE, options, {}, { options = it }, {}, {})
            }
        } }
        val switches = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        compose.onAllNodes(switches).assertCountEquals(6)
        compose.onNode(switches and hasText("AMOLED crna")).performScrollTo()
        val pixels = compose.onNodeWithTag("appearance-switch-${R.string.theme_amoled}", useUnmergedTree = true).captureToImage().toPixelMap()
        val thumb = pixels[(pixels.width * .72f).toInt(), pixels.height / 2]
        assertTrue("AMOLED active thumb stays dark", thumb.red + thumb.green + thumb.blue < .3f)
        screenshot("appearance-display-large-dark")
        compose.onNode(switches and hasText("AMOLED crna")).assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertFalse(options.amoled) }
    }

    @Test fun wrappedSegmentsHaveEqualHeightAndKeepTheirSelection() {
        var selected by mutableIntStateOf(0)
        val labels = listOf("Sistemska podešavanja", "Svetla", "Automatska tamna tema")
        compose.setContent { SerbianLarge {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(top = 64.dp)) {
                    AppSegmentedControl(labels.map { SegmentOption(it, Icons.Default.Palette) }, selected, { selected = it }, Modifier.width(280.dp))
                }
            }
        } }
        val tabs = labels.map { compose.onNodeWithText(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        tabs.forEach { assertEquals(tabs.first().top, it.top, 1f); assertEquals(tabs.first().height, it.height, 1f) }
        compose.onNodeWithText(labels[2]).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(2, selected) }
        screenshot("segmented-large-light")
    }

    @Test fun languageChangesKeepDateResourcesAndActivityContextTogether() {
        var language by mutableStateOf(AppLanguage.ENGLISH)
        compose.setContent {
            val activityContext = LocalContext.current
            ProvideAppLocale(language) {
                assertSame("Native launchers retain the Activity context", activityContext, LocalContext.current)
                val locale = LocalConfiguration.current.locales[0]
                Column {
                    Text(stringResource(R.string.nav_settings))
                    Text(LocalDate.of(2026, 10, 8).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)))
                }
            }
        }
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithText("Thursday, 8 October").assertExists()
        compose.runOnIdle { language = AppLanguage.SERBIAN_LATIN }
        compose.onNodeWithText("Podešavanja").assertExists()
        compose.onNodeWithText("četvrtak, 8 oktobar").assertExists()
        compose.runOnIdle { language = AppLanguage.SYSTEM }
        val systemLocale = android.content.res.Resources.getSystem().configuration.locales[0]
        compose.onNodeWithText(LocalDate.of(2026, 10, 8).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", systemLocale))).assertExists()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5122-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
