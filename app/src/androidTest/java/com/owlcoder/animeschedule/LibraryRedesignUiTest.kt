package com.owlcoder.animeschedule

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.owlcoder.animeschedule.core.locale.ProvideAppLocale
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.SheetBackdropHost
import com.owlcoder.animeschedule.presentation.screens.mylist.*
import com.owlcoder.animeschedule.ui.theme.AnimeScheduleTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LibraryRedesignUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val rows = listOf(
        MalListEntry(101, "Sousou no Frieren: Beyond Journey’s End", status = WatchStatus.WATCHING,
            episodesWatched = 4, score = 8, totalEpisodes = 28),
        MalListEntry(102, "A Very Long Anime Title Across Multiple Lines", status = WatchStatus.COMPLETED,
            episodesWatched = 12, score = 9, totalEpisodes = 12),
    )

    @Composable private fun Theme(large: Boolean, dark: Boolean, content: @Composable () -> Unit) {
        ProvideAppLocale(AppLanguage.SERBIAN_LATIN) {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.35f else 1f)) {
                AnimeScheduleTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT,
                    options = ThemeOptions(palette = ThemePalette.ICE, amoled = dark), content = content)
            }
        }
    }

    @Composable private fun Library(ui: MyListUiState, onFavorites: () -> Unit = {},
        onClear: () -> Unit = {}, onIncrement: (Int) -> Unit = {}, onEdit: (Int) -> Unit = {}) {
        SheetBackdropHost {
            LoggedInList(ui, rows.size, false, "Fixture error", {}, {}, {}, {}, onIncrement, onEdit,
                {}, {}, {}, onFavorites, {}, {}, {}, {}, {}, onClear)
        }
    }

    @Test fun largeLibraryKeepsCardsVisibleAndSeparatesFiltersFromActions() {
        var ui by mutableStateOf(MyListUiState(entries = rows, allEntries = rows, isLoggedIn = true,
            activeFilter = null, statusCounts = rows.groupingBy { it.status }.eachCount()))
        var increments = 0
        var edits = 0
        compose.setContent { Theme(large = true, dark = true) {
            Library(ui, onFavorites = { ui = ui.copy(favoritesOnly = !ui.favoritesOnly) },
                onIncrement = { id -> assertEquals(101, id); increments++; ui = ui.copy(pendingIncrementIds = setOf(id)) },
                onEdit = { id -> assertEquals(101, id); edits++ })
        } }
        compose.onNodeWithTag("list-filter-menu").assertIsDisplayed()
        compose.onNodeWithTag("mylist-entry-101").assertIsDisplayed()
        compose.onNodeWithTag("list-favorites-filter").assertDoesNotExist()
        screenshot("library-large-dark")
        compose.onNodeWithTag("list-filter-menu").performClick()
        compose.onNodeWithTag("list-favorites-filter").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithTag("list-filters-apply").assertIsDisplayed()
        screenshot("library-filters-large-dark")
        compose.onNodeWithTag("list-filters-apply").performClick()
        compose.onAllNodes(isDialog()).assertCountEquals(0)
        val increment = compose.onNode(hasContentDescription("+1 odgledana epizoda") and hasAnyAncestor(hasTestTag("mylist-entry-101")))
        increment.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick().assertIsNotEnabled()
        compose.onNode(hasContentDescription("Uredi status na listi") and hasAnyAncestor(hasTestTag("mylist-entry-101")))
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, increments); assertEquals(1, edits) }
    }

    @Test fun emptyStatusProvidesAResetToTheExistingLibrary() {
        var ui by mutableStateOf(MyListUiState(entries = emptyList(), allEntries = rows.drop(1), isLoggedIn = true,
            activeFilter = WatchStatus.WATCHING))
        compose.setContent { Theme(large = false, dark = false) {
            Library(ui, onClear = { ui = ui.copy(activeFilter = null, entries = rows.drop(1)) })
        } }
        compose.onNodeWithText("Ukloni filtere").assertIsDisplayed().performClick()
        compose.onNodeWithTag("mylist-entry-102").assertIsDisplayed()
        screenshot("library-light")
    }

    @Test fun guestLoginAndBenefitsRemainReachableWithLargeText() {
        var logins = 0
        compose.setContent { Theme(large = true, dark = false) { NotLoggedInState { logins++ } } }
        compose.onNodeWithText("Prijavi se").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, logins) }
        screenshot("library-guest-large-light")
        compose.onNodeWithTag("mylist-sign-in").performScrollToIndex(3)
        compose.onNodeWithText("Sačuvaj ocene").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        if (!QaCapture.enabled) return
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "qa-5126-$name.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
