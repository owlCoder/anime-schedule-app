package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.time.epochSecondsToLocalDate
import com.owlcoder.animeschedule.core.time.todayRangeUtc
import com.owlcoder.animeschedule.core.time.tomorrowRangeUtc
import com.owlcoder.animeschedule.core.time.weekRangeUtc
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.api.jikan.JikanApiService
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.mapper.toAiringEpisodeEntity
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_SECONDS = 30 * 60L

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val aniListDataSource: AniListRemoteDataSource,
    private val jikanApiService: JikanApiService
) : ScheduleRepository {

    override fun getTodaySchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> {
        val (from, to) = todayRangeUtc(zoneId)
        return buildScheduleFlow(from, to)
    }

    override fun getTomorrowSchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> {
        val (from, to) = tomorrowRangeUtc(zoneId)
        return buildScheduleFlow(from, to)
    }

    override fun getWeekSchedule(zoneId: ZoneId): Flow<AppResult<List<ScheduleDay>>> {
        val (from, to) = weekRangeUtc(zoneId)
        return combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries ->
            val malMap = malEntries.associate { it.malId to it.toDomain() }
            val domainEpisodes = episodes.map { it.toDomain(it.malId?.let { mid -> malMap[mid] }) }
            val grouped = domainEpisodes.groupBy { epochSecondsToLocalDate(it.airingAtEpochSeconds, zoneId) }
            AppResult.Success(
                grouped.entries.sortedBy { it.key }.map { ScheduleDay(it.key, it.value) }
            )
        }
    }

    override suspend fun refreshSchedule(zoneId: ZoneId) {
        val (from, to) = weekRangeUtc(zoneId)
        val nowEpoch = Instant.now().epochSecond
        val result = aniListDataSource.getAiringSchedule(from, to)
        if (result is AppResult.Success) {
            val entities = result.data.mapNotNull { it.toEntity(nowEpoch) }
            airingEpisodeDao.upsertAll(entities)
            return
        }
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        for (day in days) {
            runCatching {
                val response = jikanApiService.getSchedule(day)
                val entities = response.data.mapNotNull { it.toAiringEpisodeEntity(day, nowEpoch) }
                airingEpisodeDao.upsertAll(entities)
            }
        }
    }

    private fun buildScheduleFlow(from: Long, to: Long): Flow<AppResult<List<AiringEpisode>>> =
        combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries ->
            val malMap = malEntries.associate { it.malId to it.toDomain() }
            AppResult.Success(episodes.map { it.toDomain(it.malId?.let { mid -> malMap[mid] }) })
        }
}
