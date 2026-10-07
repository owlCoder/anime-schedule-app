package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.domain.model.effectiveZoneId
import java.io.IOException
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class WatchToolsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val preferences: UserPreferencesDataStore,
) {
    private val json = Json { ignoreUnknownKeys = true }
    internal fun key(prefs: Preferences) = stringPreferencesKey(
        "watch_tools_v1_${
            prefs[stringPreferencesKey("mal_username")]?.lowercase(Locale.ROOT)
                ?.takeIf { it.isNotBlank() } ?: "guest"
        }"
    )

    internal fun read(prefs: Preferences): WatchTools = runCatching {
        json.decodeFromString<WatchTools>(prefs[key(prefs)] ?: "{}")
    }.getOrDefault(WatchTools())

    val data =
        dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map(::read).distinctUntilChanged().flowOn(Dispatchers.Default)

    private suspend fun edit(transform: (WatchTools) -> WatchTools) {
        dataStore.edit { prefs -> prefs[key(prefs)] = json.encodeToString(transform(read(prefs))) }
    }

    suspend fun toggleFavorite(id: Int) = edit {
        it.copy(favorites = if (id in it.favorites) it.favorites - id else it.favorites + id)
    }

    suspend fun setNotificationMuted(anilistId: Int, title: String, muted: Boolean) = edit {
        if (anilistId <= 0) it else it.copy(mutedNotifications = if (muted) it.mutedNotifications + (anilistId to title.trim().take(500)) else it.mutedNotifications - anilistId)
    }

    suspend fun setNote(id: Int, note: String) = edit {
        val trimmed = note.trim().take(2000)
        it.copy(notes = if (trimmed.isBlank()) it.notes - id else it.notes + (id to trimmed))
    }

    suspend fun setTags(id: Int, text: String) = edit {
        val tags = normalizedTags(text)
        it.copy(tags = if (tags.isEmpty()) it.tags - id else it.tags + (id to tags))
    }

    suspend fun setEpisodeMinutes(minutes: Int) = edit { it.copy(episodeMinutes = minutes.coerceIn(1, 180)) }

    suspend fun togglePin(id: Int) = edit {
        if (id <= 0) it else it.copy(pinned = if (id in it.pinned) it.pinned - id else it.pinned + id)
    }
    suspend fun setDailyGoal(goal: Int) = edit { it.copy(dailyGoal = goal.coerceIn(0, 50)) }
    suspend fun setDurationOverride(id: Int, minutes: Int?) = edit {
        if (id <= 0) it else it.copy(durationOverrides = if (minutes == null) it.durationOverrides - id else it.durationOverrides + (id to minutes.coerceIn(1, 180)))
    }
    suspend fun saveView(view: SavedListView) = edit {
        val value = view.normalized()
        if (value.name.isBlank()) it else it.copy(savedViews = (listOf(value) + it.savedViews.filterNot { existing -> existing.name.equals(value.name, true) }).take(8))
    }
    suspend fun deleteView(name: String) = edit { it.copy(savedViews = it.savedViews.filterNot { view -> view.name.equals(name, true) }) }

    suspend fun setMarkers(ids: Set<Int>, favorite: Boolean? = null, pin: Boolean? = null) = edit { it.withMarkers(ids, favorite, pin) }
    suspend fun renameTag(old: String, replacement: String?) = edit { it.renameTag(old, replacement) }

    suspend fun setWeeklyGoal(goal: Int) = edit { it.copy(weeklyGoal = goal.coerceIn(0, 100)) }
    suspend fun clearActivity() = edit { it.copy(activity = emptyList()) }

    suspend fun recordProgress(id: Int, title: String, before: Int, after: Int) {
        val today = LocalDate.now(preferences.userPreferencesFlow.first().effectiveZoneId)
        edit { it.withProgress(id, title, before, after, today) }
    }
}
