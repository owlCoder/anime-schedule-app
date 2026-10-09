package com.owlcoder.animeschedule.domain.repository

import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.AppLanguage
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.model.CharacterDetail
import com.owlcoder.animeschedule.domain.model.LoginState
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.domain.model.ThemeMode
import com.owlcoder.animeschedule.domain.model.UserPreferences
import com.owlcoder.animeschedule.domain.model.WatchSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.ZoneId

interface ScheduleRepository {
    /** The seven local days starting at [today] in [zoneId], grouped by local date; never fails. */
    fun getWeekSchedule(zoneId: ZoneId, today: LocalDate): Flow<List<ScheduleDay>>

    /** Fetches the current week from the remote provider into the local cache. */
    suspend fun refreshSchedule(zoneId: ZoneId): AppResult<Unit>
}

interface AnimeDetailRepository {
    fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>>
    suspend fun getCharacterDetail(characterId: Int): AppResult<CharacterDetail>
}

interface MalRepository {
    val syncState: Flow<com.owlcoder.animeschedule.domain.model.MalSyncState>
        get() = kotlinx.coroutines.flow.flowOf(com.owlcoder.animeschedule.domain.model.MalSyncState())
    val undoChange: Flow<com.owlcoder.animeschedule.domain.model.UndoListChange?>
        get() = kotlinx.coroutines.flow.flowOf(null)
    suspend fun undoListChange(id: Long): AppResult<Unit> = AppResult.Error(com.owlcoder.animeschedule.core.result.AppError.NoCache)
    fun dismissUndo(id: Long) {}
    suspend fun retrySync(): Boolean = refreshUserList(force = true)
    fun getUserList(): Flow<List<MalListEntry>>
    suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit>
    suspend fun incrementEpisode(animeId: Int): AppResult<Unit>
    suspend fun removeListEntry(animeId: Int): AppResult<Unit>

    /** @return true when the sync completed (or was skipped as fresh or because nobody is
     *  signed in); false when it failed and the local cache was left untouched. */
    suspend fun refreshUserList(force: Boolean = false): Boolean

    /** Pushes offline-queued list mutations to MAL. @return true when the queue is empty
     *  afterwards. */
    suspend fun flushPendingUpdates(): Boolean
}

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val username: Flow<String>
    val avatarUrl: Flow<String>

    /** Progress of the browser-based sign-in; shared by every screen that offers sign-in. */
    val loginState: StateFlow<LoginState>

    /** Starts a sign-in attempt and returns the authorization URL to open in the browser. */
    fun beginLogin(): String

    /**
     * Completes a sign-in from the OAuth redirect, validating [state] against the attempt.
     * @return true when the account is now signed in.
     */
    suspend fun completeLogin(code: String, state: String?): Boolean

    /** The redirect came back without an authorization code (the user declined access). */
    fun loginDenied()

    /** The browser was dismissed without any redirect arriving. */
    fun loginAbandoned()

    suspend fun logout()
}

interface SettingsRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setTimezoneId(timezoneId: String)
    suspend fun setThemeOptions(options: com.owlcoder.animeschedule.domain.model.ThemeOptions)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationOffset(minutes: Int)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setCacheRetentionDays(days: Int)
}

interface SearchRepository {
    val recentSearches: Flow<List<String>>

    suspend fun searchAnime(query: String, page: Int = 0): AppResult<SearchPage>
    suspend fun saveRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun removeRecentSearch(query: String)
}

interface SeasonalRepository {
    suspend fun getSeasonalAnime(season: AnimeSeason, year: Int): AppResult<List<SeasonalAnimeItem>>
}

interface NotificationRepository {
    fun getAll(): Flow<List<AppNotification>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markRead(id: Int)
    suspend fun setRead(id: Int, read: Boolean) {
        if (read) markRead(id) else throw UnsupportedOperationException("Unread status is unsupported by this provider")
    }
    suspend fun markRead(ids: List<Int>) { ids.distinct().forEach { markRead(it) } }
    suspend fun markAllRead()
    suspend fun deleteRead(): Int
}

interface WatchSourceRepository {
    fun getAll(): Flow<List<WatchSource>>
    suspend fun add(name: String, urlTemplate: String, faviconUrl: String?, openExternally: Boolean)
    suspend fun update(source: WatchSource)
    suspend fun delete(source: WatchSource)
}
