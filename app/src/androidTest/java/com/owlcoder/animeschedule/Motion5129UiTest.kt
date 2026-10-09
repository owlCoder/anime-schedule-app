package com.owlcoder.animeschedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

class Motion5129UiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun rapidContentChangesSettleAndReducedMotionDoesNotLeaveASizeSpring() {
        var tall by mutableStateOf(false)
        var reduce by mutableStateOf(false)
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT, options = ThemeOptions(reduceMotion = reduce)) {
                val motion = LocalMotionPolicy.current
                Box(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                    AnimatedContent(tall, Modifier.testTag("motion-content"),
                        transitionSpec = { motion.contentTransform() }, label = "motion-regression") { expanded ->
                        Box(Modifier.width(200.dp).height(if (expanded) 180.dp else 80.dp)
                            .background(MaterialTheme.colorScheme.surface)) {
                            Text(if (expanded) "Expanded content" else "Compact content")
                        }
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        repeat(5) {
            compose.runOnIdle { tall = !tall }
            compose.mainClock.advanceTimeBy(32)
        }
        compose.mainClock.advanceTimeBy(320)
        compose.onNodeWithTag("motion-content").assertHeightIsEqualTo(180.dp)
        compose.onNodeWithText("Expanded content").assertIsDisplayed()
        compose.onNodeWithText("Compact content").assertDoesNotExist()
        compose.runOnIdle { reduce = true }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { tall = false }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("motion-content").assertHeightIsEqualTo(80.dp)
        compose.onNodeWithText("Compact content").assertIsDisplayed()
        compose.onNodeWithText("Expanded content").assertDoesNotExist()
    }

    @Test fun reducedMotionPanelsCloseThroughTheirHeaderAndReopenCleanly() {
        var open by mutableStateOf(false)
        var dismissals = 0
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.DARK, options = ThemeOptions(reduceMotion = true)) {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    AppButton("Open panel", { open = true })
                    if (open) AppSheet({ open = false; dismissals++ }, title = "Panel") {
                        Text("Panel content")
                        AppSwitch(true, {}, Modifier.testTag("motion-switch"))
                    }
                }
            }
        }
        repeat(4) {
            compose.onNodeWithText("Open panel").performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(1)
            compose.onNodeWithTag("motion-switch").assertHeightIsAtLeast(48.dp)
            compose.onNode(hasContentDescription(instrumentation.targetContext.getString(android.R.string.cancel))
                and hasAnyAncestor(isDialog())).performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            compose.onNodeWithText("Open panel").assertIsDisplayed()
        }
        compose.runOnIdle { assertEquals(4, dismissals) }
    }

    @Test fun panelDismissalWaitsForItsExitAndThenRemovesTheWholeDialog() {
        var open by mutableStateOf(false)
        var dismissals = 0
        compose.setContent {
            AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
                Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    AppButton("Open animated panel", { open = true })
                    if (open) AppSheet({ open = false; dismissals++ }, title = "Animated panel") {
                        Text("Animated content")
                    }
                }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Open animated panel").performClick()
        compose.mainClock.advanceTimeBy(352)
        compose.onNode(hasContentDescription(instrumentation.targetContext.getString(android.R.string.cancel))
            and hasAnyAncestor(isDialog())).performClick()
        compose.mainClock.advanceTimeBy(64)
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.runOnIdle { assertEquals(0, dismissals) }
        compose.mainClock.advanceTimeBy(288)
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.runOnIdle { assertEquals(1, dismissals) }
        compose.onNodeWithText("Open animated panel").assertIsDisplayed()
    }

    @Test fun systemReducedMotionChangesAreObservedWithoutRestartingTheComposition() {
        val setting = "animator_duration_scale"
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command)
            .use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText().trim() }
        val original = shell("settings get global $setting")
        try {
            shell("settings put global $setting 1.0")
            var reduced = true
            compose.setContent {
                AnimeScheduleTheme {
                    val policy = LocalMotionPolicy.current
                    SideEffect { reduced = policy.reduceMotion }
                    Text(if (policy.reduceMotion) "Motion reduced" else "Motion enabled")
                }
            }
            compose.waitUntil(5000) { !reduced }
            shell("settings put global $setting 0.0")
            compose.waitUntil(5000) { reduced }
            compose.onNodeWithText("Motion reduced").assertIsDisplayed()
            shell("settings put global $setting 1.0")
            compose.waitUntil(5000) { !reduced }
            compose.onNodeWithText("Motion enabled").assertIsDisplayed()
        } finally {
            if (original == "null") shell("settings delete global $setting")
            else shell("settings put global $setting $original")
        }
    }
}
