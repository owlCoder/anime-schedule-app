package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val MAL_LOGGED_IN = booleanPreferencesKey("mal_logged_in")
        val MAL_USERNAME = stringPreferencesKey("mal_username")
        val MAL_AVATAR_URL = stringPreferencesKey("mal_avatar_url")
        val LAST_SYNC = longPreferencesKey("last_schedule_sync_epoch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_OFFSET = intPreferencesKey("notification_offset_minutes")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            timezoneId = prefs[Keys.TIMEZONE_ID] ?: "",
            malLoggedIn = prefs[Keys.MAL_LOGGED_IN] ?: false,
            malUsername = prefs[Keys.MAL_USERNAME] ?: "",
            malAvatarUrl = prefs[Keys.MAL_AVATAR_URL] ?: "",
            lastScheduleSyncEpoch = prefs[Keys.LAST_SYNC] ?: 0L,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationOffsetMinutes = prefs[Keys.NOTIFICATION_OFFSET] ?: 0,
            accentColor = runCatching { AccentColor.valueOf(prefs[Keys.ACCENT_COLOR] ?: "") }.getOrDefault(AccentColor.TELEGRAM_BLUE),
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            appLanguage = runCatching { AppLanguage.valueOf(prefs[Keys.APP_LANGUAGE] ?: "") }.getOrDefault(AppLanguage.SYSTEM)
        )
    }

    suspend fun setTimezoneId(timezoneId: String) {
        dataStore.edit { it[Keys.TIMEZONE_ID] = timezoneId }
    }

    suspend fun setMalLoggedIn(loggedIn: Boolean, username: String = "", avatarUrl: String = "") {
        dataStore.edit {
            it[Keys.MAL_LOGGED_IN] = loggedIn
            it[Keys.MAL_USERNAME] = username
            it[Keys.MAL_AVATAR_URL] = avatarUrl
        }
    }

    suspend fun setLastSyncEpoch(epoch: Long) {
        dataStore.edit { it[Keys.LAST_SYNC] = epoch }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationOffset(minutes: Int) {
        dataStore.edit { it[Keys.NOTIFICATION_OFFSET] = minutes }
    }

    suspend fun setAccentColor(color: AccentColor) {
        dataStore.edit { it[Keys.ACCENT_COLOR] = color.name }
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }
}
