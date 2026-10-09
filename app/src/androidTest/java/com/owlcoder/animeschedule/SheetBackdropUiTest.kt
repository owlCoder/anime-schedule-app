package com.owlcoder.animeschedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import androidx.test.espresso.Espresso
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class SheetBackdropUiTest {
    @get:Rule val compose = createComposeRule()

    private fun contrast(): Float {
        val pixels = compose.onNodeWithTag("backdrop-pattern").captureToImage().toPixelMap()
        val samples = (pixels.width / 4 until pixels.width * 3 / 4 step 3).map { pixels[it, pixels.height / 2].red }
        return samples.max() - samples.min()
    }

    @Composable private fun Pattern() {
        Canvas(Modifier.fillMaxWidth().height(128.dp).testTag("backdrop-pattern")) {
            val stripe = 8.dp.toPx()
            for (i in 0..(size.width / stripe).toInt()) drawRect(if (i % 2 == 0) Color.White else Color.Black, Offset(i * stripe, 0f), Size(stripe, size.height))
        }
    }

    @Test fun backgroundStaysSharpDuringContentChangesAndRepeatedDismissals() {
        var title by mutableStateOf<String?>(null)
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.DARK) {
            SheetBackdropHost(Modifier.fillMaxSize()) {
                Column { Pattern(); AppButton("Open", { title = "First" }, Modifier.testTag("open-sheet")) }
                title?.let { value -> AppSheet({ title = null }, title = value) {
                    Text(value)
                    AppButton("Change content", { title = "Second" }, Modifier.testTag("change-sheet"))
                    AppButton("Close", { title = null }, Modifier.testTag("close-sheet"))
                } }
            }
        } }
        repeat(3) { index ->
            assertTrue("Background starts sharp", contrast() > .8f)
            compose.onNodeWithTag("open-sheet").performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(1)
            assertTrue("Open overlay keeps the background sharp", contrast() > .8f)
            compose.onNodeWithTag("change-sheet").performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(1)
            compose.onNodeWithTag("close-sheet").assertIsDisplayed()
            assertTrue("Changing content keeps the background sharp", contrast() > .8f)
            if (index == 1) Espresso.pressBack()
            else compose.onNodeWithTag("close-sheet").performClick()
            compose.onAllNodes(isDialog()).assertCountEquals(0)
            assertTrue("Dismissal leaves the background sharp", contrast() > .8f)
        }
    }

    @Test fun stackedModalsNeverBlurActivityContent() {
        var first by mutableStateOf(false)
        var second by mutableStateOf(false)
        compose.setContent { AnimeScheduleTheme(themeMode = ThemeMode.LIGHT) {
            SheetBackdropHost(Modifier.fillMaxSize()) {
                Pattern()
                if (first) AppSheet({ first = false }, title = "First") { Text("First modal") }
                if (second) AppSheet({ second = false }, title = "Second") { Text("Second modal") }
            }
        } }
        assertTrue(contrast() > .8f)
        compose.runOnIdle { first = true; second = true }
        compose.onAllNodes(isDialog()).assertCountEquals(2)
        assertTrue(contrast() > .8f)
        compose.runOnIdle { first = false }
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        assertTrue("The remaining modal keeps the background sharp", contrast() > .8f)
        compose.runOnIdle { second = false }
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        assertTrue("Last modal leaves the background sharp", contrast() > .8f)
    }
}
