package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.screens.mylist.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WorkspaceExpansionUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun text(id: Int) = instrumentation.targetContext.getString(id)
    private val entries = listOf(
        MalListEntry(101, "Alpha Adventure", status = WatchStatus.WATCHING, episodesWatched = 4, score = 8, totalEpisodes = 12),
        MalListEntry(102, "Beta Journey", status = WatchStatus.WATCHING, episodesWatched = 0, score = 0, totalEpisodes = 24),
    )
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, options = ThemeOptions(palette = if (dark) ThemePalette.NEON else ThemePalette.SAKURA), content = content) }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        instrumentation.uiAutomation.waitForIdle(400, 5000)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-560-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun ratingSliderAppliesInclusiveRange() {
        var applied = 0 to 10
        show { RatingRangeSheet(0 to 10, { min, max -> applied = min to max }, {}) }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        compose.onNodeWithTag("rating-apply").performClick()
        assertEquals(7 to 10, applied)
        screenshot("rating-range")
    }

    @Test fun bulkSelectionAppliesOnlySelectedLocalMarkers() {
        var updated = WatchTools(favorites = setOf(99))
        show { BulkOrganizeSheet(entries, { ids, favorite, pin -> updated = updated.withMarkers(ids, favorite, pin) }, {}) }
        compose.onNodeWithTag("bulk-all").performClick()
        compose.onNodeWithTag("bulk-102").performScrollTo().performClick()
        compose.onNodeWithTag("bulk-101").assertIsOn()
        compose.onNodeWithTag("bulk-102").assertIsOff()
        compose.onNodeWithTag("bulk-action-0").performScrollTo().performClick()
        assertEquals(setOf(99,101), updated.favorites)
        compose.onNodeWithTag("bulk-action-2").performClick()
        assertEquals(setOf(101), updated.pinned)
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("bulk-organize")
    }

    @Test fun tagRenameUpdatesSavedViewsAndRemovalNeedsInlineConfirmation() {
        var tools by mutableStateOf(WatchTools(tags = mapOf(101 to setOf("Action"),102 to setOf("Weekend")), savedViews = listOf(SavedListView("Weekend view",tag="Action"))))
        show(true) { ManageTagsSheet(tools, { old, replacement -> tools = tools.renameTag(old, replacement) }, {}) }
        compose.onNodeWithTag("tag-edit-Action").performClick()
        compose.onNodeWithTag("tag-replacement").performTextReplacement("Weekend")
        compose.onNodeWithTag("tag-rename-save").performClick()
        compose.waitUntil { tools.savedViews.single().tag == "Weekend" }
        assertEquals(setOf("Weekend"), tools.tags[101])
        compose.onNodeWithContentDescription(instrumentation.targetContext.getString(R.string.delete_tag,"Weekend")).performClick()
        assertTrue(tools.tags.isNotEmpty())
        compose.onAllNodes(isDialog()).assertCountEquals(1)
        screenshot("tag-confirmation")
        compose.onNodeWithTag("tag-delete-confirm").performClick()
        compose.waitUntil { tools.tags.isEmpty() }
        assertNull(tools.savedViews.single().tag)
    }

    @Test fun plannerModesAndTitleSelectionChangeSuggestionsWithoutChangingProgress() {
        show(true) { WatchPlannerSheet(entries, WatchTools(), {}, {}) }
        compose.onNodeWithTag("planner-budget-120").performClick()
        compose.onNodeWithTag("planner-style").performScrollTo().performClick()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-style-FOCUS"))
        compose.onNodeWithTag("planner-style-FOCUS").performClick()
        compose.onNodeWithTag("plan-101").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("plan-102").assertDoesNotExist()
        compose.onNodeWithTag("planner-choose").performScrollTo().performClick()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("planner-include-101"))
        compose.onNodeWithTag("planner-include-101").performClick()
        compose.onNodeWithTag("planner-include-101").assertIsOff()
        compose.onNodeWithTag("planner-list").performScrollToNode(hasTestTag("plan-102"))
        compose.onNodeWithTag("plan-102").assertIsDisplayed()
        compose.onNodeWithTag("plan-101").assertDoesNotExist()
        assertEquals(4, entries.first().episodesWatched)
        compose.onNodeWithTag("planner-list").performScrollToIndex(0)
        screenshot("planner-focus")
    }

    @Test fun comparisonShowsProgressAndIndividualRemainingTime() {
        show { CompareAnimeSheet(entries, WatchTools(durationOverrides = mapOf(101 to 12)), {}) }
        compose.onNodeWithTag("compare-101").performClick()
        compose.onNodeWithTag("compare-102").performClick()
        compose.onNodeWithTag("compare-list").performScrollToIndex(0)
        compose.onNodeWithText("4/12").assertIsDisplayed()
        compose.onNodeWithText("0/24").assertIsDisplayed()
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.compare_time, 1, 36)).assertIsDisplayed()
        screenshot("comparison")
    }

    @Test fun historyExportUsesVisibleRangeAndSearch() {
        val today = LocalDate.now()
        val recent = WatchActivity(101,"Alpha Adventure",today.toString(),2,6)
        val old = WatchActivity(102,"Beta Journey",today.minusDays(40).toString(),1,1)
        var exported = emptyList<WatchActivity>()
        show { WatchHistorySheet(WatchTools(activity=listOf(recent,old)), {}, {}, {}, onExport={ exported=it }) }
        compose.onNodeWithTag("history-range-LAST_30").performScrollTo().performClick()
        compose.onNodeWithTag("history-export").performScrollTo().performClick()
        assertEquals(listOf(recent), exported)
        compose.onNodeWithTag("history-range-LAST_90").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("history-search"))).performTextInput("Beta")
        compose.onNodeWithTag("history-export").performScrollTo().performClick()
        assertEquals(listOf(old), exported)
        // Close IME before capturing the sheet.
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("history-search"))).performImeAction()
        screenshot("history-range")
    }

    @Test fun backlogShowsPerStatusTimeEstimatesAndUnknownCounts() {
        val data = entries + MalListEntry(103,"Unknown", status=WatchStatus.PLAN_TO_WATCH, episodesWatched=0, score=0,totalEpisodes=null)
        show(true) { BacklogSheet(data, WatchTools(dailyGoal=4, durationOverrides=mapOf(101 to 12)), {}) }
        compose.onNodeWithText(instrumentation.targetContext.resources.getQuantityString(R.plurals.backlog_episodes_time,32,32,11,12)).assertIsDisplayed()
        compose.onNodeWithText(instrumentation.targetContext.resources.getQuantityString(R.plurals.backlog_days_estimate,8,8,4)).assertIsDisplayed()
        compose.onNodeWithText(instrumentation.targetContext.getString(R.string.backlog_unknown,1)).performScrollTo().assertIsDisplayed()
        screenshot("backlog")
    }
}
