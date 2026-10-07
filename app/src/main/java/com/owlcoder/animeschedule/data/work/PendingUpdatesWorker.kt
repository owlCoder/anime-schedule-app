package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.owlcoder.animeschedule.domain.repository.MalRepository
import kotlinx.coroutines.flow.first

/**
 * Flushes MAL list mutations queued while offline. Enqueued with a CONNECTED constraint the
 * moment an update fails on the network, so it runs as soon as connectivity returns —
 * WorkManager persists the request across process death and reboots.
 */
@HiltWorker
class PendingUpdatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val malRepository: MalRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val allFlushed = malRepository.flushPendingUpdates()
        if (allFlushed) return Result.success()
        val state = malRepository.syncState.first()
        return when {
            // Reauthentication and rejected values require a user action, not backoff retries.
            !state.loggedIn || (state.pendingCount > 0 && state.pendingCount == state.rejectedCount) -> Result.failure()
            runAttemptCount < 5 -> Result.retry()
            else -> Result.failure()
        }
    }
}
