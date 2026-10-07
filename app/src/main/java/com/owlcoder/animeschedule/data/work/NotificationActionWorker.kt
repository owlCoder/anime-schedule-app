package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.*
import kotlinx.coroutines.CancellationException

@HiltWorker
class NotificationActionWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted params: WorkerParameters,
    private val actions: NotificationActions,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val id = inputData.getInt(NotificationActionReceiver.ID, 0)
        val generation = inputData.getInt(NotificationActionReceiver.GENERATION, -1)
        val token = inputData.getString(NotificationActionReceiver.TOKEN).orEmpty()
        val done = when (inputData.getString(NotificationActionReceiver.ACTION)) {
            NotificationActionReceiver.INCREMENT -> actions.increment(id, token)
            NotificationActionReceiver.SNOOZE -> actions.snooze(id, generation, token)
            else -> false
        }
        if (done) Result.success() else Result.failure()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}

@HiltWorker
class NotificationReminderWorker @AssistedInject constructor(
    @Assisted context: Context, @Assisted params: WorkerParameters,
    private val actions: NotificationActions,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        actions.remind(inputData.getInt(NotificationActionReceiver.ID, 0), inputData.getInt(NotificationActionReceiver.GENERATION, -1), inputData.getString(NotificationActionReceiver.TOKEN).orEmpty())
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}
