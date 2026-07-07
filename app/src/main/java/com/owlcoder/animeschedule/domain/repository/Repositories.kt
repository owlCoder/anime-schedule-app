package com.owlcoder.animeschedule.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.local.datastore.AccentColor
import com.owlcoder.animeschedule.data.local.datastore.AppLanguage
import com.owlcoder.animeschedule.data.local.datastore.ThemeMode
import com.owlcoder.animeschedule.data.local.datastore.UserPreferences
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import java.time.ZoneId

interface ScheduleRepository {
    fun getTodaySchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>>
    fun getTomorrowSchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>>
    fun getWeekSchedule(zoneId: ZoneId): Flow<AppResult<List<ScheduleDay>>>
    suspend fun refreshSchedule(zoneId: ZoneId)
}

interface AnimeDetailRepository {
    fun getAnimeDetail(animeId: Int): Flow<AppResult<AnimeDetail>>
}

interface MalRepository {
    fun getUserList(): Flow<AppResult<List<MalListEntry>>>
    suspend fun updateListEntry(animeId: Int, update: MalListUpdate): AppResult<Unit>
    suspend fun incrementEpisode(animeId: Int): AppResult<Unit>
    suspend fun refreshUserList(force: Boolean = false)
}

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val username: Flow<String>
    val avatarUrl: Flow<String>
    fun buildAuthUri(): Triple<Uri, String, String>
    suspend fun handleOAuthCallback(code: String, verifier: String): Boolean
    suspend fun logout()
}

interface SettingsRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setTimezoneId(timezoneId: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationOffset(minutes: Int)
    suspend fun setAccentColor(color: AccentColor)
    suspend fun setAppLanguage(language: AppLanguage)
    fun getEffectiveZoneId(prefs: UserPreferences): ZoneId
}

interface SearchRepository {
    suspend fun searchAnime(query: String, page: Int = 0): AppResult<List<AnimeSearchResult>>
}

interface SeasonalRepository {
    suspend fun getSeasonalAnime(season: AnimeSeason, year: Int): AppResult<List<SeasonalAnimeItem>>
}

interface NotificationRepository {
    fun getAll(): Flow<List<AppNotification>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markRead(id: Int)
    suspend fun markAllRead()
    suspend fun createNotification(episode: AiringEpisode)
}
