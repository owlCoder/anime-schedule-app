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

class PersonalBackupStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun `restoration replaces only active account metadata and appearance while preserving authentication`() = runTest {
        val job = SupervisorJob()
        val backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(temporary.root,"backup.preferences_pb") }
        val prefs = UserPreferencesDataStore(backing); val tools = WatchToolsStore(backing, prefs); val backup = PersonalBackupStore(backing,prefs,tools)
        try {
            prefs.setMalLoggedIn(true,"First"); tools.setTags(101,"Akcija, Drama"); tools.setNote(101,"Prva beleška"); tools.setEpisodeMinutes(45)
            prefs.saveAppearancePreset(AppearancePreset("Ocean", ThemeMode.DARK, options = ThemeOptions(palette = ThemePalette.OCEAN)))
            val exported = PersonalBackup.decode(backup.export())
            prefs.setMalLoggedIn(true,"Second"); tools.toggleFavorite(202)
            prefs.setMalLoggedIn(true,"FIRST"); tools.setTags(101,""); tools.setNote(101,"Promena"); tools.toggleFavorite(303)
            backup.restore(exported)
            assertEquals(mapOf(101 to setOf("Akcija","Drama")), tools.data.first().tags)
            assertEquals("Prva beleška", tools.data.first().notes[101]); assertEquals(45, tools.data.first().episodeMinutes)
            assertTrue(tools.data.first().favorites.isEmpty()); assertEquals("FIRST", prefs.userPreferencesFlow.first().malUsername)
            assertTrue(prefs.userPreferencesFlow.first().malLoggedIn); assertEquals("Ocean", prefs.userPreferencesFlow.first().appearancePresets.single().name)
            prefs.setMalLoggedIn(true,"Second"); assertEquals(setOf(202), tools.data.first().favorites)
        } finally { job.cancelAndJoin() }
    }
    @Test fun `saved looks replace duplicate names cap at eight and survive reopening`() = runTest {
        val file = File(temporary.root,"looks.preferences_pb"); var job = SupervisorJob()
        var backing = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }; var prefs = UserPreferencesDataStore(backing)
        repeat(10) { prefs.saveAppearancePreset(AppearancePreset("Look $it")) }
        prefs.saveAppearancePreset(AppearancePreset("look 9",ThemeMode.DARK,AccentColor.GREEN,ThemeOptions(palette=ThemePalette.LAVENDER,scheduled=true,compactLayout=true)))
        assertEquals(8,prefs.userPreferencesFlow.first().appearancePresets.size)
        job.cancelAndJoin(); job=SupervisorJob(); backing=PreferenceDataStoreFactory.create(scope=CoroutineScope(job+Dispatchers.IO)){file}; prefs=UserPreferencesDataStore(backing)
        try {
            val looks=prefs.userPreferencesFlow.first().appearancePresets; assertEquals(1,looks.count{it.name.equals("look 9",true)})
            prefs.applyAppearancePreset(looks.first()); val applied=prefs.userPreferencesFlow.first()
            assertEquals(ThemeMode.DARK,applied.themeMode); assertEquals(AccentColor.GREEN,applied.accentColor); assertEquals(ThemePalette.LAVENDER,applied.themeOptions.palette); assertTrue(applied.themeOptions.scheduled)
            prefs.deleteAppearancePreset("look 9"); assertEquals(7,prefs.userPreferencesFlow.first().appearancePresets.size)
        } finally { job.cancelAndJoin() }
    }
}
