package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.owlcoder.animeschedule.domain.model.ThemeOptions
import com.owlcoder.animeschedule.domain.model.normalized
import com.owlcoder.animeschedule.domain.model.withLegacyAccent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Upgrade only appearance keys; account, library and notification preferences stay intact. */
class AppearanceMigration : DataMigration<Preferences> {
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR] != null ||
            currentData[UserPreferencesDataStore.Keys.LEGACY_APPEARANCE_PRESETS] != null ||
            currentData[UserPreferencesDataStore.Keys.THEME_OPTIONS]?.contains("\"dynamicColors\"") == true

    override suspend fun migrate(currentData: Preferences): Preferences = currentData.toMutablePreferences().apply {
        val options = runCatching {
            json.decodeFromString<ThemeOptions>(currentData[UserPreferencesDataStore.Keys.THEME_OPTIONS] ?: "{}")
        }.getOrDefault(ThemeOptions()).normalized().withLegacyAccent(currentData[UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR])
        this[UserPreferencesDataStore.Keys.THEME_OPTIONS] = Json.encodeToString(options)
        remove(UserPreferencesDataStore.Keys.LEGACY_ACCENT_COLOR)
        remove(UserPreferencesDataStore.Keys.LEGACY_APPEARANCE_PRESETS)
    }

    override suspend fun cleanUp() = Unit
}
