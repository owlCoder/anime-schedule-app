package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.CacheRetentionPolicy
import com.owlcoder.animeschedule.domain.model.normalized
import com.owlcoder.animeschedule.domain.model.ThemeOptions
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.ThemePalette
import com.owlcoder.animeschedule.domain.model.withLegacyAccent
import com.owlcoder.animeschedule.domain.model.QuietHours
import com.owlcoder.animeschedule.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json { ignoreUnknownKeys = true }

    internal object Keys {
        val TIMEZONE_ID = stringPreferencesKey("timezone_id")
        val MAL_LOGGED_IN = booleanPreferencesKey("mal_logged_in")
        val MAL_USERNAME = stringPreferencesKey("mal_username")
        val MAL_AVATAR_URL = stringPreferencesKey("mal_avatar_url")
        val LAST_MAL_LIST_SYNC = longPreferencesKey("last_mal_list_sync_epoch_ms")
        val LAST_MAL_SYNC_SUCCESS = longPreferencesKey("last_mal_sync_success_epoch_ms")
        val LEGACY_APPEARANCE_PRESETS = stringPreferencesKey("appearance_presets_v1")
        val THEME_OPTIONS = stringPreferencesKey("theme_options_v1")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val QUIET_HOURS = stringPreferencesKey("notification_quiet_hours_v1")
        val NOTIFICATION_OFFSET = intPreferencesKey("notification_offset_minutes")
        val LEGACY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CACHE_RETENTION_DAYS = intPreferencesKey("cache_retention_days")
        val LAST_IMAGE_CACHE_CLEAR = longPreferencesKey("last_image_cache_clear_epoch_seconds")
    }

    /**
     * The backing DataStore also holds unrelated keys (recent searches, sync timestamps), so
     * every write re-emits [Preferences]. Collapsing equal [UserPreferences] keeps downstream
     * pipelines from restarting when nothing they read has changed.
     */
    // A corrupt preferences file must not take the whole app down; fall back to defaults.
    private val safeData: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val userPreferencesFlow: Flow<UserPreferences> = safeData.map(::read).distinctUntilChanged()
    val lastMalSyncSuccess: Flow<Long> = safeData.map {
        it[Keys.LAST_MAL_SYNC_SUCCESS] ?: it[Keys.LAST_MAL_LIST_SYNC] ?: 0L
    }.distinctUntilChanged()
    suspend fun setLastMalSyncSuccess(epochMs: Long) {
        dataStore.edit { it[Keys.LAST_MAL_SYNC_SUCCESS] = epochMs }
    }
    suspend fun expireMalSession() {
        dataStore.edit { it[Keys.MAL_LOGGED_IN] = false }
    }

    internal fun read(prefs: Preferences): UserPreferences {
        val storedLanguage = runCatching {
            AppLanguage.valueOf(prefs[Keys.APP_LANGUAGE] ?: "")
        }.getOrDefault(AppLanguage.ENGLISH)
        val appLanguage = if (storedLanguage == AppLanguage.SYSTEM) {
            AppLanguage.ENGLISH
        } else {
            storedLanguage
        }
        return UserPreferences(
            timezoneId = prefs[Keys.TIMEZONE_ID] ?: "",
            malLoggedIn = prefs[Keys.MAL_LOGGED_IN] ?: false,
            malUsername = prefs[Keys.MAL_USERNAME] ?: "",
            malAvatarUrl = prefs[Keys.MAL_AVATAR_URL] ?: "",
            themeOptions = runCatching { json.decodeFromString<ThemeOptions>(prefs[Keys.THEME_OPTIONS] ?: "{}").normalized() }.getOrDefault(ThemeOptions()).withLegacyAccent(prefs[Keys.LEGACY_ACCENT_COLOR]),
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationOffsetMinutes = prefs[Keys.NOTIFICATION_OFFSET] ?: 0,
            quietHours = runCatching { json.decodeFromString<QuietHours>(prefs[Keys.QUIET_HOURS] ?: "{}").normalized() }.getOrDefault(QuietHours()),
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            appLanguage = appLanguage,
            cacheRetentionDays = CacheRetentionPolicy.normalizeRetentionDays(
                prefs[Keys.CACHE_RETENTION_DAYS] ?: CacheRetentionPolicy.DEFAULT_RETENTION_DAYS
            )
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

    // Persisted (not in-memory) so the MAL list cache TTL survives process death — otherwise
    // every cold start re-downloaded the whole list.
    suspend fun getLastMalListSyncEpochMs(): Long =
        safeData.first()[Keys.LAST_MAL_LIST_SYNC] ?: 0L

    suspend fun setLastMalListSyncEpochMs(epochMs: Long) {
        dataStore.edit { it[Keys.LAST_MAL_LIST_SYNC] = epochMs }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeThemeOptions(options: ThemeOptions) {
        this[Keys.THEME_OPTIONS] = Json.encodeToString(options.normalized())
        remove(Keys.LEGACY_ACCENT_COLOR)
        remove(Keys.LEGACY_APPEARANCE_PRESETS)
    }

    suspend fun setThemeOptions(options: ThemeOptions) {
        dataStore.edit { it.writeThemeOptions(options) }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        dataStore.edit { it.writeThemeOptions(read(it).themeOptions.copy(palette = palette)) }
    }

    suspend fun resetAppearance() {
        dataStore.edit {
            it[Keys.THEME_MODE] = ThemeMode.SYSTEM.name
            it.writeThemeOptions(read(it).themeOptions.copy(palette = ThemePalette.CLASSIC, scheduled = false))
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit {
            it[Keys.THEME_MODE] = mode.name
            it.writeThemeOptions(read(it).themeOptions.copy(scheduled = false))
        }
    }

    suspend fun completeOnboarding(mode: ThemeMode, palette: ThemePalette, language: AppLanguage,
        notificationsEnabled: Boolean, offsetMinutes: Int) {
        dataStore.edit {
            it[Keys.THEME_MODE] = mode.name
            it.writeThemeOptions(read(it).themeOptions.copy(palette = palette, scheduled = false))
            it[Keys.APP_LANGUAGE] = language.name
            it[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            it[Keys.NOTIFICATION_OFFSET] = offsetMinutes
            it[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationOffset(minutes: Int) {
        dataStore.edit { it[Keys.NOTIFICATION_OFFSET] = minutes }
    }

    suspend fun setQuietHours(value: QuietHours) {
        dataStore.edit { it[Keys.QUIET_HOURS] = Json.encodeToString(value.normalized()) }
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setCacheRetentionDays(days: Int) {
        dataStore.edit {
            it[Keys.CACHE_RETENTION_DAYS] = CacheRetentionPolicy.normalizeRetentionDays(days)
        }
    }

    suspend fun getLastImageCacheClearEpochSeconds(): Long =
        safeData.first()[Keys.LAST_IMAGE_CACHE_CLEAR] ?: 0L

    suspend fun setLastImageCacheClearEpochSeconds(epochSeconds: Long) {
        dataStore.edit { it[Keys.LAST_IMAGE_CACHE_CLEAR] = epochSeconds }
    }
}
