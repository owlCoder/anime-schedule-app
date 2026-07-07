package com.owlcoder.animeschedule.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_RECENT = 10

@Singleton
class RecentSearchesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val key = stringPreferencesKey("recent_searches")
    private val json = Json { ignoreUnknownKeys = true }

    val recentSearchesFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun save(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(query) + current.filter { it != query }).take(MAX_RECENT)
            prefs[key] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
