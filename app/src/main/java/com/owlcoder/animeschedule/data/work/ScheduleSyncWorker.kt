package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.usecase.RefreshMalListUseCase
import com.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import kotlinx.coroutines.flow.first

@HiltWorker
class ScheduleSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshScheduleUseCase: RefreshScheduleUseCase,
    private val refreshMalListUseCase: RefreshMalListUseCase,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = settingsRepository.userPreferencesFlow.first()
            val zoneId = settingsRepository.getEffectiveZoneId(prefs)
            refreshScheduleUseCase(zoneId)
            if (prefs.malLoggedIn) {
                // Keep the local MAL list in step with edits made on the website — otherwise
                // they only arrived when the user happened to open the My List tab.
                runCatching { refreshMalListUseCase(force = true) }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
