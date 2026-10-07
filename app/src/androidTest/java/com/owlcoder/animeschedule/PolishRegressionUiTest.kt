package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PolishRegressionUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun searchIconAndPaddingFocusInputAndSubmitClearsFocus() {
        var value by mutableStateOf("")
        var submitted = ""
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(18.dp)) {
                    AppSearchField(value, { value = it }, Modifier.testTag("polish-search"), "Search this schedule", Icons.Default.Search,
                        onClear = { value = "" }, onSearch = { submitted = value })
                }
            }
        }
        val input = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("polish-search")))
        input.performTouchInput { click(Offset(8f, centerY)) }
        input.assertIsFocused().performTextInput("Alpha")
        input.assertContentDescriptionEquals("Search this schedule")
        screenshot("search-focused")
        input.performImeAction()
        input.assertIsNotFocused()
        assertEquals("Alpha", submitted)
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(R.string.search_clear_query)).performClick()
        assertEquals("", value)
        input.assertContentDescriptionEquals("Search this schedule")
    }

    @Test fun disabledSearchCannotBeFocusedByItsDecoration() {
        compose.setContent {
            AnimeScheduleTheme {
                Column(Modifier.statusBarsPadding().padding(18.dp)) {
                    AppSearchField("", {}, Modifier.testTag("disabled-search"), "Unavailable search", Icons.Default.Search, enabled = false)
                }
            }
        }
        compose.onNodeWithContentDescription("Unavailable search").performTouchInput { click(Offset(8f, centerY)) }
            .assert(SemanticsMatcher("does not own input focus") { it.config.getOrNull(SemanticsProperties.Focused) != true })
            .assertIsNotEnabled()
    }

    @Test fun largeTextHeaderKeepsFullTitleAndToolbarActionsVisible() {
        var clicked = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
                AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(palette = ThemePalette.NEON)) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(18.dp)) {
                        AppLargeHeader("Raspored emitovanja", subtitle = "Četvrtak, 8. oktobar", trailingContent = {
                            GlassToolbarGroup {
                                GlassToolbarButton(Icons.Default.AutoAwesome, "Seasonal", {})
                                GlassToolbarButton(Icons.Default.Notifications, "Notifications", {})
                                GlassToolbarButton(Icons.Default.Tune, "Filters", { clicked = true })
                            }
                        })
                        GlassIconButton(Icons.Default.Refresh, "Refresh", {}, Modifier.testTag("touch-target"))
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Raspored emitovanja").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        screenshot("header-large-text")
        val layout = layouts.single()
        // Semantics can remeasure a paragraph at the available width while retaining the
        // text's smaller measured size. Check visible glyph bounds and text, not that width.
        assertFalse((0 until layout.lineCount).any(layout::isLineEllipsized))
        assertEquals(layout.layoutInput.text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        assertTrue((0 until layout.lineCount).all { layout.getLineRight(it) <= layout.size.width + 1f })
        assertFalse(layout.didOverflowHeight)
        compose.onNodeWithContentDescription("Seasonal").assertIsDisplayed()
        compose.onNodeWithContentDescription("Notifications").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filters").assertIsDisplayed().performClick()
        assertTrue(clicked)
        compose.onNodeWithTag("touch-target").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        screenshot("header-large-text")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle(); instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-581-$name.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
