package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

private const val MAX_RECENT = 10

@Singleton
class RecentSearchesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val key = stringPreferencesKey("recent_searches")
    private val json = Json { ignoreUnknownKeys = true }

    val recentSearchesFlow: Flow<List<String>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs[key]?.let(::decode).orEmpty() }
        .distinctUntilChanged()

    suspend fun save(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let(::decode).orEmpty()
            val updated = (listOf(query) + current.filter { it != query }).take(MAX_RECENT)
            prefs[key] = json.encodeToString(updated)
        }
    }

    // A malformed stored value is treated as "no history" rather than failing the search screen.
    private fun decode(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
