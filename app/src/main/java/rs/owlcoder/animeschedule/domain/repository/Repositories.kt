package rs.owlcoder.animeschedule.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.local.datastore.AccentColor
import rs.owlcoder.animeschedule.data.local.datastore.ThemeMode
import rs.owlcoder.animeschedule.data.local.datastore.UserPreferences
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.AnimeDetail
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult
import rs.owlcoder.animeschedule.domain.model.AppNotification
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.model.ScheduleDay
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
    fun buildAuthUri(): Pair<Uri, String>
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
    fun getEffectiveZoneId(prefs: UserPreferences): ZoneId
}

interface SearchRepository {
    suspend fun searchAnime(query: String, page: Int = 0): AppResult<List<AnimeSearchResult>>
}

interface NotificationRepository {
    fun getAll(): Flow<List<AppNotification>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markRead(id: Int)
    suspend fun markAllRead()
    suspend fun createNotification(episode: AiringEpisode)
}
