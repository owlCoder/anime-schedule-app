package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.owlcoder.animeschedule.domain.model.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppearanceMigrationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `migration preserves display and account state while replacing legacy accent and presets`() = runTest {
        val file = File(temporary.root, "legacy.preferences_pb")
        var job = SupervisorJob()
        var data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
        data.edit {
            it[UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR] = "GREEN"
            it[UserPreferencesDataStore.Keys.LEGACY_APPEARANCE_PRESETS] = "old saved data"
            it[UserPreferencesDataStore.Keys.THEME_OPTIONS] = """{"palette":"CLASSIC","amoled":false,"compactLayout":true,"scheduled":true,"dynamicColors":true}"""
            it[UserPreferencesDataStore.Keys.THEME_MODE] = "DARK"
            it[UserPreferencesDataStore.Keys.MAL_LOGGED_IN] = true
            it[UserPreferencesDataStore.Keys.MAL_USERNAME] = "Fixture"
        }
        job.cancelAndJoin(); job = SupervisorJob()
        data = PreferenceDataStoreFactory.create(migrations = listOf(AppearanceMigration()), scope = CoroutineScope(job + Dispatchers.IO)) { file }
        try {
            val prefs = UserPreferencesDataStore(data)
            val value = prefs.userPreferencesFlow.first()
            assertEquals(ThemePalette.FOREST, value.themeOptions.palette)
            assertEquals(ThemeMode.DARK, value.themeMode)
            assertFalse(value.themeOptions.amoled); assertTrue(value.themeOptions.compactLayout); assertTrue(value.themeOptions.scheduled)
            assertTrue(value.malLoggedIn); assertEquals("Fixture", value.malUsername)
            assertNull(data.data.first()[UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR])
            assertNull(data.data.first()[UserPreferencesDataStore.Keys.LEGACY_APPEARANCE_PRESETS])
            assertFalse(AppearanceMigration().shouldMigrate(data.data.first()))
            prefs.setThemePalette(ThemePalette.CLASSIC)
            assertEquals(ThemePalette.CLASSIC, prefs.userPreferencesFlow.first().themeOptions.palette)
            prefs.setThemeMode(ThemeMode.LIGHT)
            assertFalse(prefs.userPreferencesFlow.first().themeOptions.scheduled)
        } finally { job.cancelAndJoin() }
    }

    @Test fun `explicit palette wins over old accent and unknown accent falls back safely`() = runTest {
        assertEquals(ThemePalette.RUBY, ThemeOptions(palette = ThemePalette.RUBY).withLegacyAccent("GREEN").palette)
        assertEquals(ThemePalette.CLASSIC, ThemeOptions().withLegacyAccent("bad").palette)
        val migrated = AppearanceMigration().migrate(mutablePreferencesOf(
            UserPreferencesDataStore.Keys.THEME_OPTIONS to "broken json",
            UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR to "TEAL",
        ))
        assertFalse(AppearanceMigration().shouldMigrate(migrated))
        assertTrue(migrated[UserPreferencesDataStore.Keys.THEME_OPTIONS]!!.contains("MINT"))
    }

    @Test fun `old backups import current appearance but omit saved looks from new exports`() {
        val backup = PersonalBackup.decode("""{"schemaVersion":1,"tools":{"favorites":[5]},"appearance":{"name":"Current","mode":"DARK","accent":"RED","options":{"palette":"CLASSIC","compactLayout":true}},"presets":[{"name":"Old look"}]}""")
        assertEquals(2, backup.schemaVersion)
        assertEquals(setOf(5), backup.tools.favorites)
        assertEquals(ThemePalette.RUBY, backup.appearance.options.palette)
        assertTrue(backup.appearance.options.compactLayout)
        assertEquals(ThemeMode.DARK, backup.appearance.mode)
        assertFalse(backup.encode().contains("presets")); assertFalse(backup.encode().contains("accent"))
        assertEquals(backup, PersonalBackup.decode(backup.encode()))
    }

    @Test fun `onboarding saves palette and mode atomically and appearance reset preserves display`() = runTest {
        val job = SupervisorJob()
        val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { File(temporary.root, "onboarding.preferences_pb") }
        val prefs = UserPreferencesDataStore(data)
        try {
            prefs.setThemeOptions(ThemeOptions(highContrast = true, compactLayout = true))
            prefs.completeOnboarding(ThemeMode.DARK, ThemePalette.AMETHYST, AppLanguage.SERBIAN_LATIN, false, -15)
            val complete = prefs.userPreferencesFlow.first()
            assertTrue(complete.onboardingDone); assertEquals(ThemeMode.DARK, complete.themeMode)
            assertEquals(ThemePalette.AMETHYST, complete.themeOptions.palette)
            assertEquals(AppLanguage.SERBIAN_LATIN, complete.appLanguage)
            assertFalse(complete.notificationsEnabled); assertEquals(-15, complete.notificationOffsetMinutes)
            prefs.resetAppearance()
            val reset = prefs.userPreferencesFlow.first()
            assertEquals(ThemeMode.SYSTEM, reset.themeMode); assertEquals(ThemePalette.CLASSIC, reset.themeOptions.palette)
            assertTrue(reset.themeOptions.highContrast); assertTrue(reset.themeOptions.compactLayout)
        } finally { job.cancelAndJoin() }
    }
}
