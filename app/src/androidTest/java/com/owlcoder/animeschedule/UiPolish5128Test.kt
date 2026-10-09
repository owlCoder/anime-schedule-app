package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.locale.ProvideAppLocale
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.AppSearchField
import com.owlcoder.animeschedule.presentation.screens.detail.DetailLoadingState
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UiPolish5128Test {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun sharedSearchPreservesRequestedFocusSubmitClearAndDisabledStateWithLargeText() {
        var value by mutableStateOf("Dugačak naziv anime serije")
        var enabled by mutableStateOf(true)
        var focused = false
        var submitted = ""
        val requester = FocusRequester()
        compose.setContent { ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
                AnimeScheduleTheme(themeMode = ThemeMode.LIGHT, options = ThemeOptions(palette = ThemePalette.MINT)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
                        AppSearchField(value, { value = it }, Modifier.width(280.dp), "Pretraži anime…", Icons.Default.Search,
                            onClear = { value = "" }, onSearch = { submitted = value }, enabled = enabled,
                            focusRequester = requester, onFocusChanged = { focused = it })
                    }
                    LaunchedEffect(Unit) { requester.requestFocus() }
                }
            }
        } }
        val field = compose.onNode(hasSetTextAction())
        field.assertIsFocused().performImeAction()
        compose.runOnIdle { assertEquals(value, submitted); assertFalse(focused) }
        field.assertIsNotFocused()
        screenshot("shared-search-large-light")
        val clear = compose.onNodeWithContentDescription("Obriši pretragu")
        clear.assertHeightIsAtLeast(48.dp)
        compose.runOnIdle { enabled = false }
        clear.assertIsNotEnabled()
        compose.runOnIdle { enabled = true }
        clear.performClick()
        field.assertTextEquals("")
        compose.runOnIdle { assertEquals("", value) }
    }

    @Test fun detailLoadingOffersBackBeforeTheNetworkRequestCompletes() {
        var backCount = 0
        compose.setContent { ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(reduceMotion = true)) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    DetailLoadingState(Modifier.fillMaxSize(), onBack = { backCount++ })
                }
            }
        } }
        compose.onNodeWithTag("detail-loading").assertIsDisplayed()
        compose.onNodeWithTag("detail-loading-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, backCount) }
        screenshot("detail-loading-back-dark")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5128-$name.png").outputStream()
            .use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
