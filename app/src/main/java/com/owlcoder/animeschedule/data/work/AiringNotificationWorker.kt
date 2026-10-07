package com.owlcoder.animeschedule.data.work

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import com.owlcoder.animeschedule.data.api.mal.auth.MalSession
import androidx.room.withTransaction
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.NotificationDao
import com.owlcoder.animeschedule.data.local.db.NotificationEntity
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.domain.model.effectiveZoneId
import com.owlcoder.animeschedule.domain.model.WatchStatus
import coil3.request.allowHardware
import coil3.toBitmap

@HiltWorker
class AiringNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val watchToolsStore: com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore,
    private val notificationActionsDao: com.owlcoder.animeschedule.data.local.db.NotificationActionDao,
    private val notificationActions: NotificationActions,
    private val poster: NotificationPoster,
    private val database: com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase,
    private val session: MalSession,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
        postDueNotifications()
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        Log.e(TAG, "Notification check failed", e)
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private suspend fun postDueNotifications() {
        notificationActions.recoverSnoozes()
        val (ownerEpoch, prefs) = session.mutex.withLock {
            session.epoch to userPreferencesDataStore.userPreferencesFlow.first()
        }
        if (!prefs.notificationsEnabled || !prefs.malLoggedIn) return

        val muted = watchToolsStore.data.first().mutedNotifications.keys
        val now = System.currentTimeMillis() / 1000L
        val existingIds = notificationDao.getAllIds().toSet()
        // The first run catches up on the last day. Afterwards the window is deliberately much
        // wider than the 15 minute period: Doze can postpone periodic work by hours, and an
        // episode that aired in the gap must still notify. Episodes already notified are
        // skipped by id, so a generous window costs nothing.
        val windowStart = now - if (existingIds.isEmpty()) FIRST_RUN_LOOKBACK_SECONDS else LOOKBACK_SECONDS
        // Positive offset = notify after airing, negative = before. Episodes qualify when
        // (airingAt + offset) falls inside the window.
        val offsetSeconds = prefs.notificationOffsetMinutes * 60L

        val watchingMalIds = malListEntryDao.getAll().first()
            .filter { it.status == WatchStatus.WATCHING.malValue }
            .associateBy { it.malId }
        if (watchingMalIds.isEmpty()) return

        // The query returns episodes in airing order, so notifications are posted chronologically.
        val dueEpisodes = airingEpisodeDao
            .getAiringEpisodesInRange(windowStart - offsetSeconds, now - offsetSeconds)
            .first()
            .filter { episode ->
                episode.malId in watchingMalIds && episode.airingId !in existingIds
            }

        for (episode in dueEpisodes) {
            val row = NotificationEntity(
                id = episode.airingId,
                animeId = episode.animeId,
                title = episode.title,
                episode = episode.episode,
                coverImageUrl = episode.coverImageUrl,
                airingAtEpochSeconds = episode.airingAtEpochSeconds,
                isRead = false,
                createdAtEpochSeconds = now
            )
            val shouldAlert = com.owlcoder.animeschedule.domain.model.shouldPostSystemAlert(
                episode.animeId, muted, prefs.quietHours, java.time.Instant.ofEpochSecond(now), prefs.effectiveZoneId,
                java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds + offsetSeconds),
            ) && NotificationManagerCompat.from(context).areNotificationsEnabled()
            val cover = if (shouldAlert) episode.coverImageUrl?.let { loadBitmap(it) } else null
            session.mutex.withLock {
                val currentPrefs = userPreferencesDataStore.userPreferencesFlow.first()
                if (session.epoch != ownerEpoch || !currentPrefs.malLoggedIn) return@withLock
                val entry = malListEntryDao.getByAnimeId(episode.malId ?: return@withLock)?.takeIf { it.status == WatchStatus.WATCHING.malValue } ?: return@withLock
                val fresh = database.withTransaction {
                    if (notificationActionsDao.get(episode.airingId)?.progressHandled == true) false
                    else {
                        notificationActionsDao.insert(com.owlcoder.animeschedule.data.local.db.NotificationActionEntity(episode.airingId, entry.malId))
                        notificationDao.insertOnce(row) != -1L
                    }
                }
                if (!fresh) return@withLock
                val receipt = notificationActionsDao.get(episode.airingId) ?: return@withLock
                if (currentPrefs.notificationsEnabled && com.owlcoder.animeschedule.domain.model.shouldPostSystemAlert(
                    episode.animeId, watchToolsStore.data.first().mutedNotifications.keys, currentPrefs.quietHours, java.time.Instant.ofEpochSecond(now), currentPrefs.effectiveZoneId,
                    // Doze may defer a quiet-hour broadcast until after quiet hours have ended.
                    java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds + offsetSeconds),
                ) && NotificationManagerCompat.from(context).areNotificationsEnabled()) poster.post(row, canIncrement = entry.totalEpisodes?.takeIf { it > 0 }?.let { entry.numEpisodesWatched < it } ?: true,
                    cover = cover, actionToken = receipt.actionToken)
            }
        }
    }

    /** Coil reports failures through its result, so a missing cover simply yields no bitmap. */
    private suspend fun loadBitmap(url: String): Bitmap? {
        // Reuse the application loader so notification work does not create another cache.
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        return SingletonImageLoader.get(context).execute(request).image?.toBitmap()
    }

    companion object {
        const val CHANNEL_ID = "airing_episodes"
        private const val TAG = "AiringNotifWorker"
        private const val MAX_ATTEMPTS = 3
        private const val LOOKBACK_SECONDS = 2 * 60 * 60L
        private const val FIRST_RUN_LOOKBACK_SECONDS = 24 * 60 * 60L
    }
}
