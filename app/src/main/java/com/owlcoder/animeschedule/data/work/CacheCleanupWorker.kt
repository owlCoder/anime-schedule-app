package com.owlcoder.animeschedule.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cacheMaintenance: CacheMaintenance
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        cacheMaintenance.run()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.e(TAG, "Cache cleanup failed", error)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    private companion object {
        const val TAG = "CacheCleanupWorker"
    }
}
