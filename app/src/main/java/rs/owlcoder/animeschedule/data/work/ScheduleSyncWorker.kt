package rs.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
import rs.owlcoder.animeschedule.domain.usecase.RefreshScheduleUseCase
import kotlinx.coroutines.flow.first

@HiltWorker
class ScheduleSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshScheduleUseCase: RefreshScheduleUseCase,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = settingsRepository.userPreferencesFlow.first()
            val zoneId = settingsRepository.getEffectiveZoneId(prefs)
            refreshScheduleUseCase(zoneId)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
