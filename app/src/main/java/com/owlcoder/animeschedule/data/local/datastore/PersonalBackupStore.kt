package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.owlcoder.animeschedule.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class PersonalBackupStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val preferences: UserPreferencesDataStore,
    private val tools: WatchToolsStore,
) {
    suspend fun export(): String {
        val snapshot = dataStore.data.first()
        val prefs = preferences.read(snapshot)
        return PersonalBackup(
            tools = tools.read(snapshot),
            appearance = AppearancePreset("Current", prefs.themeMode, prefs.accentColor, prefs.themeOptions),
            presets = prefs.appearancePresets,
        ).encode().also { require(it.toByteArray(Charsets.UTF_8).size <= 2_000_000) { "Backup is too large" } }
    }

    /** One transaction restores the current account's local data and appearance together. */
    suspend fun restore(backup: PersonalBackup) {
        val value = backup.normalized()
        dataStore.edit { prefs ->
            prefs[tools.key(prefs)] = Json.encodeToString(value.tools)
            prefs[UserPreferencesDataStore.Keys.THEME_MODE] = value.appearance.mode.name
            prefs[UserPreferencesDataStore.Keys.ACCENT_COLOR] = value.appearance.accent.name
            prefs[UserPreferencesDataStore.Keys.THEME_OPTIONS] = Json.encodeToString(value.appearance.options)
            prefs[UserPreferencesDataStore.Keys.APPEARANCE_PRESETS] = Json.encodeToString(value.presets)
        }
    }
}
