package rs.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        val LAST_SYNC = longPreferencesKey("last_schedule_sync_epoch")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            timezoneId = prefs[Keys.TIMEZONE_ID] ?: "",
            malLoggedIn = prefs[Keys.MAL_LOGGED_IN] ?: false,
            malUsername = prefs[Keys.MAL_USERNAME] ?: "",
            lastScheduleSyncEpoch = prefs[Keys.LAST_SYNC] ?: 0L,
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        )
    }

    suspend fun setTimezoneId(timezoneId: String) {
        dataStore.edit { it[Keys.TIMEZONE_ID] = timezoneId }
    }

    suspend fun setMalLoggedIn(loggedIn: Boolean, username: String = "") {
        dataStore.edit {
            it[Keys.MAL_LOGGED_IN] = loggedIn
            it[Keys.MAL_USERNAME] = username
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
}
