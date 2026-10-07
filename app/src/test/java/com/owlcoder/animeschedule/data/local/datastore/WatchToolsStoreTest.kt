package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.owlcoder.animeschedule.domain.model.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WatchToolsStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun `notes favorites goal and theme survive reopening and are isolated between accounts`() = runTest {
        val file = File(temporary.root, "watch.preferences_pb")
        var job = SupervisorJob()
        var backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        var prefs = UserPreferencesDataStore(backing)
        var tools = WatchToolsStore(backing, prefs)
        prefs.setMalLoggedIn(true, "First")
        tools.toggleFavorite(101)
        tools.setNote(101, "  Pauza na epizodi 4  ")
        tools.setWeeklyGoal(20)
        tools.togglePin(101)
        tools.setDailyGoal(7)
        tools.setDurationOverride(101, 48)
        tools.saveView(SavedListView("Weekend", smartFilter = SmartListFilter.PINNED))
        prefs.setThemeOptions(ThemeOptions(palette = ThemePalette.SAKURA, highContrast = true, reduceMotion = true))
        prefs.setMalLoggedIn(true, "Second")
        assertTrue(tools.data.first().favorites.isEmpty())
        assertTrue(tools.data.first().notes.isEmpty())
        assertTrue(tools.data.first().pinned.isEmpty())
        assertTrue(tools.data.first().durationOverrides.isEmpty())
        assertTrue(tools.data.first().savedViews.isEmpty())
        tools.toggleFavorite(202)
        prefs.setMalLoggedIn(true, "FIRST")
        job.cancelAndJoin()
        job = SupervisorJob()
        backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        prefs = UserPreferencesDataStore(backing)
        tools = WatchToolsStore(backing, prefs)
        try {
            assertEquals(setOf(101), tools.data.first().favorites)
            assertEquals("Pauza na epizodi 4", tools.data.first().notes[101])
            assertEquals(20, tools.data.first().weeklyGoal)
            assertEquals(setOf(101), tools.data.first().pinned)
            assertEquals(7, tools.data.first().dailyGoal)
            assertEquals(mapOf(101 to 48), tools.data.first().durationOverrides)
            assertEquals(SmartListFilter.PINNED, tools.data.first().savedViews.single().smartFilter)
            tools.saveView(SavedListView("weekend", query = "Beta"))
            assertEquals(1, tools.data.first().savedViews.size)
            assertEquals("Beta", tools.data.first().savedViews.single().query)
            tools.deleteView("WEEKEND")
            assertTrue(tools.data.first().savedViews.isEmpty())
            tools.setDurationOverride(101, null)
            assertTrue(tools.data.first().durationOverrides.isEmpty())
            tools.setDailyGoal(900)
            assertEquals(50, tools.data.first().dailyGoal)
            assertEquals(ThemePalette.SAKURA, prefs.userPreferencesFlow.first().themeOptions.palette)
            prefs.setMalLoggedIn(false)
            assertTrue(tools.data.first().favorites.isEmpty())
            tools.setWeeklyGoal(900)
            assertEquals(100, tools.data.first().weeklyGoal)
            tools.setWeeklyGoal(-1)
            assertEquals(0, tools.data.first().weeklyGoal)
        } finally { job.cancelAndJoin() }
    }
    @Test fun `bulk markers and tag changes remain account scoped after reopening`() = runTest {
        val file = File(temporary.root, "bulk.preferences_pb")
        var job = SupervisorJob()
        var backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        var prefs = UserPreferencesDataStore(backing)
        var tools = WatchToolsStore(backing, prefs)
        prefs.setMalLoggedIn(true,"First")
        tools.setMarkers(setOf(1,2), favorite=true, pin=true)
        tools.setTags(1,"Action, Weekend")
        tools.saveView(SavedListView("Tagged",tag="Action",minimumScore=7,maximumScore=10,sort="WATCH_TIME"))
        tools.renameTag("action","Weekend")
        prefs.setMalLoggedIn(true,"Second")
        assertTrue(tools.data.first().favorites.isEmpty()); assertTrue(tools.data.first().tags.isEmpty())
        tools.setMarkers(setOf(3),favorite=true)
        prefs.setMalLoggedIn(true,"FIRST")
        job.cancelAndJoin()
        job = SupervisorJob()
        backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        prefs = UserPreferencesDataStore(backing); tools = WatchToolsStore(backing,prefs)
        try {
            assertEquals(setOf(1,2),tools.data.first().favorites)
            assertEquals(setOf(1,2),tools.data.first().pinned)
            assertEquals(setOf("Weekend"),tools.data.first().tags[1])
            assertEquals("Weekend",tools.data.first().savedViews.single().tag)
            assertEquals(7,tools.data.first().savedViews.single().minimumScore)
            tools.renameTag("Weekend",null)
            assertTrue(tools.data.first().tags.isEmpty()); assertNull(tools.data.first().savedViews.single().tag)
            tools.setMarkers(setOf(1),favorite=false,pin=false)
            assertEquals(setOf(2),tools.data.first().favorites); assertEquals(setOf(2),tools.data.first().pinned)
        } finally { job.cancelAndJoin() }
    }

}
