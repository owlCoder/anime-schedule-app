package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import com.owlcoder.animeschedule.domain.model.effectiveZoneId

@HiltWorker
class ScheduleSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val malRepository: MalRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = settingsRepository.userPreferencesFlow.first()
        val scheduleResult = scheduleRepository.refreshSchedule(prefs.effectiveZoneId)

        // Keep the local MAL list in step with edits made on the website. It skips itself when
        // nobody is signed in, and never throws for ordinary failures.
        malRepository.refreshUserList(force = true)

        // Only a failed schedule fetch is worth a WorkManager retry; the next periodic run
        // covers the MAL list.
        return when {
            scheduleResult is AppResult.Success -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
