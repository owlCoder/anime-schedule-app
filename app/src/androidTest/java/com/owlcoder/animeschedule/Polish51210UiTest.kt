package com.owlcoder.animeschedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.ThemeOptions
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Polish51210UiTest {
    @get:Rule val compose = createComposeRule()
    private val cancel = InstrumentationRegistry.getInstrumentation().targetContext.getString(android.R.string.cancel)

    @Test fun nestedPageReturnsToItsSavedInputAndScrollInsideTheSameDialog() {
        var child by mutableStateOf(false)
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
                AppSheet({}, title = if (child) "Editor" else "Tools", animateSizeChanges = false,
                    onNavigateBack = { if (child) { child = false; true } else false }) {
                    SheetPageTransition(child, isRoot = { !it }) { editor ->
                        if (!editor) AppButton("Open editor", { child = true })
                        else {
                            var query by rememberSaveable { mutableStateOf("") }
                            AppSearchField(query, { query = it }, Modifier.testTag("saved-input"), "Search entries")
                            LazyColumn(Modifier.height(260.dp).testTag("saved-list")) {
                                items(30) { Text("Entry $it", Modifier.fillMaxWidth().height(48.dp)) }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Open editor").performClick()
        val input = compose.onNode(hasSetTextAction() and hasContentDescription("Search entries"))
        input.performTextInput("Saved query")
        compose.onNodeWithTag("saved-list").performScrollToNode(hasText("Entry 27"))
        compose.onNodeWithText("Entry 27").assertIsDisplayed()
        compose.onNodeWithContentDescription(cancel).performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithText("Open editor").performClick()
        input.assertTextEquals("Saved query")
        compose.onNodeWithText("Entry 27").assertIsDisplayed()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test fun reducedMotionNestedPageSettlesWithoutAnOutgoingPageOrSizeDelay() {
        var child by mutableStateOf(false)
        compose.setContent {
            AnimeScheduleTheme(options = ThemeOptions(reduceMotion = true)) {
                SheetPageTransition(child, isRoot = { !it }, Modifier.testTag("page-size")) { editor ->
                    Box(Modifier.height(if (editor) 180.dp else 80.dp)) {
                        Text(if (editor) "Editor page" else "Tools page")
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { child = true }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("page-size").assertHeightIsEqualTo(180.dp)
        compose.onNodeWithText("Editor page").assertIsDisplayed()
        compose.onNodeWithText("Tools page").assertDoesNotExist()
    }

    @Test fun panelToastCompletesItsExitBeforeItIsRemoved() {
        val toast = ToastController()
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK) {
                ToastHost(toast) {
                    AppSheet({}, title = "Panel") {
                        AppButton("Show message", { toast.success("Saved inside panel") })
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(352)
        compose.onNodeWithText("Show message").performClick()
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithTag("app-toast").assertIsDisplayed()
        compose.runOnIdle { toast.dismiss() }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("app-toast").assertExists()
        compose.mainClock.advanceTimeBy(288)
        compose.onNodeWithTag("app-toast").assertDoesNotExist()
        compose.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test fun replacementToastCannotExecuteTheOutgoingUndoAction() {
        val toast = ToastController()
        var undos = 0
        var dismissals = 0
        compose.setContent { AnimeScheduleTheme { ToastHost(toast) { Text("Page") } } }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { toast.show("Initial edit", ToastTone.Info,
            ToastAction(1, "Undo edit", { undos++ }, { dismissals++ })) }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle { toast.error("New message") }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("toast-action").performClick()
        compose.runOnIdle {
            assertEquals(0, undos)
            assertEquals(1, dismissals)
            assertEquals("New message", toast.current?.message)
        }
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithText("Initial edit").assertDoesNotExist()
        compose.onNodeWithText("New message").assertIsDisplayed()
        compose.onAllNodesWithTag("app-toast").assertCountEquals(1)
    }
}
