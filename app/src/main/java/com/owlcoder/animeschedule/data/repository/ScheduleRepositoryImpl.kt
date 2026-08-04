package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.time.epochSecondsToLocalDate
import com.owlcoder.animeschedule.core.time.todayRangeUtc
import com.owlcoder.animeschedule.core.time.tomorrowRangeUtc
import com.owlcoder.animeschedule.core.time.weekRangeUtc
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.offline.OfflineCatalogDataSource
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.data.provider.requireProviderData
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val aniListDataSource: AniListRemoteDataSource,
    private val providerOrchestrator: ProviderOrchestrator,
    private val offlineCatalogDataSource: OfflineCatalogDataSource
) : ScheduleRepository {

    override fun getTodaySchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> {
        val (from, to) = todayRangeUtc(zoneId)
        return buildScheduleFlow(from, to, zoneId)
    }

    override fun getTomorrowSchedule(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> {
        val (from, to) = tomorrowRangeUtc(zoneId)
        return buildScheduleFlow(from, to, zoneId)
    }

    override fun getWeekSchedule(zoneId: ZoneId): Flow<AppResult<List<ScheduleDay>>> {
        val (from, to) = weekRangeUtc(zoneId)
        return combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries -> episodes to malEntries }
            .flatMapLatest { (episodes, malEntries) ->
                val malMap = malEntries.associate { it.malId to it.toDomain() }
                val domainEpisodes = episodes.map { it.toDomain(it.malId?.let { mid -> malMap[mid] }) }
                val grouped = domainEpisodes.groupBy {
                    epochSecondsToLocalDate(it.airingAtEpochSeconds, zoneId)
                }
                val cached = AppResult.Success(
                    grouped.entries.sortedBy { it.key }.map { ScheduleDay(it.key, it.value) }
                )
                if (domainEpisodes.isNotEmpty()) {
                    flowOf(cached)
                } else {
                    offlineCatalogDataSource.observeHome(zoneId)
                }
            }
    }

    override suspend fun refreshSchedule(zoneId: ZoneId) {
        val (from, to) = weekRangeUtc(zoneId)
        val nowEpoch = Instant.now().epochSecond
        val result = providerOrchestrator.firstSuccessful(
            operation = ProviderOperation.SCHEDULE,
            calls = listOf(
                ProviderCall("AniList", isUsable = { it.isNotEmpty() }) {
                    aniListDataSource.getAiringSchedule(from, to)
                        .requireProviderData("AniList")
                        .mapNotNull { it.toEntity(nowEpoch) }
                }
            )
        )
        if (result is ProviderResult.Success && result.value.isNotEmpty()) {
            airingEpisodeDao.upsertAll(result.value)
        }
    }

    private fun buildScheduleFlow(
        from: Long,
        to: Long,
        zoneId: ZoneId
    ): Flow<AppResult<List<AiringEpisode>>> =
        combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries -> episodes to malEntries }
            .flatMapLatest { (episodes, malEntries) ->
                val malMap = malEntries.associate { it.malId to it.toDomain() }
                val cached = episodes.map { it.toDomain(it.malId?.let { mid -> malMap[mid] }) }
                if (cached.isNotEmpty()) {
                    flowOf(AppResult.Success(cached))
                } else {
                    offlineCatalogDataSource.observeHome(zoneId).map { offline ->
                        when (offline) {
                            is AppResult.Success -> AppResult.Success(
                                offline.data.flatMap { it.episodes }
                                    .filter { it.airingAtEpochSeconds in from until to }
                            )
                            is AppResult.Error -> AppResult.Error(offline.error)
                        }
                    }
                }
            }
}
