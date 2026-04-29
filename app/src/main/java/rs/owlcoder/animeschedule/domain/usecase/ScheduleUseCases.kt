package rs.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.ScheduleDay
import rs.owlcoder.animeschedule.domain.repository.ScheduleRepository
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
import java.time.ZoneId
import javax.inject.Inject

class GetTodayScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> =
        scheduleRepository.getTodaySchedule(zoneId)
}

class GetTomorrowScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    operator fun invoke(zoneId: ZoneId): Flow<AppResult<List<AiringEpisode>>> =
        scheduleRepository.getTomorrowSchedule(zoneId)
}

class GetWeekScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    operator fun invoke(zoneId: ZoneId): Flow<AppResult<List<ScheduleDay>>> =
        scheduleRepository.getWeekSchedule(zoneId)
}

class RefreshScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {
    suspend operator fun invoke(zoneId: ZoneId) = scheduleRepository.refreshSchedule(zoneId)
}
