package com.owlcoder.animeschedule.data.repository

import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.core.time.epochSecondsToLocalDate
import com.owlcoder.animeschedule.core.time.weekRangeUtc
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.provider.ProviderCall
import com.owlcoder.animeschedule.data.provider.ProviderOperation
import com.owlcoder.animeschedule.data.provider.ProviderOrchestrator
import com.owlcoder.animeschedule.data.provider.ProviderResult
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.data.provider.requireProviderData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val aniListDataSource: AniListRemoteDataSource,
    private val providerOrchestrator: ProviderOrchestrator
) : ScheduleRepository {

    override fun getWeekSchedule(zoneId: ZoneId, today: LocalDate): Flow<List<ScheduleDay>> {
        val (from, to) = weekRangeUtc(zoneId, today)
        return combine(
            airingEpisodeDao.getAiringEpisodesInRange(from, to),
            malListEntryDao.getAll()
        ) { episodes, malEntries ->
            val listEntries = malEntries.associateBy({ it.malId }, { it.toDomain() })
            episodes
                .map { it.toDomain(it.malId?.let(listEntries::get)) }
                .groupBy { epochSecondsToLocalDate(it.airingAtEpochSeconds, zoneId) }
                .toSortedMap()
                .map { (date, dayEpisodes) -> ScheduleDay(date, dayEpisodes) }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun refreshSchedule(zoneId: ZoneId, startDate: LocalDate): AppResult<Unit> {
        val (from, to) = weekRangeUtc(zoneId, startDate)
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
        return when (result) {
            is ProviderResult.Success -> {
                airingEpisodeDao.upsertAll(result.value)
                AppResult.Success(Unit)
            }
            // An empty week is not an error, but nothing is worth writing either.
            is ProviderResult.Exhausted -> if (result.failures.isEmpty()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error(AppError.Network(result.failures.last().message))
            }
        }
    }
}
