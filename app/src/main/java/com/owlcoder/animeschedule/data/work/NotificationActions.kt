package com.owlcoder.animeschedule.data.work

import android.content.Context
import androidx.work.*
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.mal.auth.MalSession
import com.owlcoder.animeschedule.data.local.datastore.*
import com.owlcoder.animeschedule.data.local.db.*
import com.owlcoder.animeschedule.data.repository.MalRepositoryImpl
import com.owlcoder.animeschedule.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationActions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MalRepositoryImpl,
    private val actions: NotificationActionDao,
    private val notifications: NotificationDao,
    private val list: MalListEntryDao,
    private val prefs: UserPreferencesDataStore,
    private val tools: WatchToolsStore,
    private val poster: NotificationPoster,
    private val session: MalSession,
) {
    suspend fun increment(id: Int, token: String): Boolean {
        val result = repository.incrementFromNotification(id, token)
        if (result is AppResult.Success) {
            session.mutex.withLock {
                // An account switch can occur after the repository releases its lock.
                if (actions.get(id)?.actionToken == token) {
                    notifications.markRead(id)
                    poster.cancel(id)
                    WorkManager.getInstance(context).cancelAllWorkByTag(reminderTag(id))
                }
            }
            return true
        }
        session.mutex.withLock {
            val receipt = actions.get(id)?.takeIf { it.actionToken == token && token.isNotEmpty() } ?: return@withLock
            notifications.getById(id)?.let { row ->
                poster.post(row, receipt.snoozeGeneration, actionToken = receipt.actionToken,
                    message = context.getString(if (!prefs.userPreferencesFlow.first().malLoggedIn) R.string.notification_action_login else R.string.notification_action_failed))
            }
        }
        return false
    }

    suspend fun snooze(id: Int, generation: Int, token: String): Boolean = session.mutex.withLock {
        if (notifications.getById(id) == null) return@withLock false
        val receipt = actions.get(id)?.takeIf { it.actionToken == token && token.isNotEmpty() } ?: return@withLock false
        if (receipt.progressHandled) return@withLock false
        // A retry after process death repairs the commit/enqueue gap without shifting the time.
        if (receipt.snoozedUntilEpochMs != null && receipt.snoozeGeneration == generation + 1) {
            scheduleReminder(receipt)
            poster.cancel(id)
            return@withLock true
        }
        val until = System.currentTimeMillis() + SNOOZE_MS
        if (actions.snooze(id, generation, until) == 0) return@withLock false
        scheduleReminder(actions.get(id)!!)
        poster.cancel(id)
        true
    }

    private fun scheduleReminder(receipt: NotificationActionEntity) {
        val delay = ((receipt.snoozedUntilEpochMs ?: return) - System.currentTimeMillis()).coerceAtLeast(0)
        WorkManager.getInstance(context).enqueueUniqueWork("${reminderTag(receipt.notificationId)}-${receipt.actionToken}-${receipt.snoozeGeneration}", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<NotificationReminderWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(reminderTag(receipt.notificationId))
                .setInputData(workDataOf(NotificationActionReceiver.ID to receipt.notificationId, NotificationActionReceiver.GENERATION to receipt.snoozeGeneration, NotificationActionReceiver.TOKEN to receipt.actionToken)).build())
    }

    suspend fun recoverSnoozes() { actions.pendingSnoozes().forEach(::scheduleReminder) }

    suspend fun remind(id: Int, generation: Int, token: String) = session.mutex.withLock {
        val receipt = actions.get(id)?.takeIf { it.actionToken == token && token.isNotEmpty() } ?: return@withLock
        val until = receipt.snoozedUntilEpochMs ?: return@withLock
        if (receipt.progressHandled || receipt.snoozeGeneration != generation || System.currentTimeMillis() < until) return@withLock
        val row = notifications.getById(id)
        val preferences = prefs.userPreferencesFlow.first()
        val entry = list.getByAnimeId(receipt.malId)
        if (row != null && preferences.malLoggedIn && preferences.notificationsEnabled && entry?.status == WatchStatus.WATCHING.malValue &&
            shouldPostSystemAlert(row.animeId, tools.data.first().mutedNotifications.keys, preferences.quietHours, Instant.now(), preferences.effectiveZoneId)) {
            poster.post(row, generation, actionToken = receipt.actionToken, canIncrement = entry.totalEpisodes?.takeIf { it > 0 }?.let { entry.numEpisodesWatched < it } ?: true)
        }
        actions.finishSnooze(id, generation)
    }

    companion object {
        const val SNOOZE_MS = 15 * 60_000L
        fun reminderTag(id: Int) = "notification-reminder-$id"
    }
}
