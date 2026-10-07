package com.owlcoder.animeschedule.data.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
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
import com.owlcoder.animeschedule.MainActivity
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.NotificationDao
import com.owlcoder.animeschedule.data.local.db.NotificationEntity
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.domain.model.effectiveZoneId
import com.owlcoder.animeschedule.domain.model.WatchStatus
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.core.net.toUri

@HiltWorker
class AiringNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationDao: NotificationDao,
    private val airingEpisodeDao: AiringEpisodeDao,
    private val malListEntryDao: MalListEntryDao,
    private val watchToolsStore: com.owlcoder.animeschedule.data.local.datastore.WatchToolsStore,
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
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        if (!prefs.notificationsEnabled) return

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
            .mapTo(HashSet()) { it.malId }
        if (watchingMalIds.isEmpty()) return

        // The query returns episodes in airing order, so notifications are posted chronologically.
        val dueEpisodes = airingEpisodeDao
            .getAiringEpisodesInRange(windowStart - offsetSeconds, now - offsetSeconds)
            .first()
            .filter { episode ->
                episode.malId in watchingMalIds && episode.airingId !in existingIds
            }

        for (episode in dueEpisodes) {
            notificationDao.upsert(
                NotificationEntity(
                    id = episode.airingId,
                    animeId = episode.animeId,
                    title = episode.title,
                    episode = episode.episode,
                    coverImageUrl = episode.coverImageUrl,
                    airingAtEpochSeconds = episode.airingAtEpochSeconds,
                    isRead = false,
                    createdAtEpochSeconds = now
                )
            )
            if (com.owlcoder.animeschedule.domain.model.shouldPostSystemAlert(
                episode.animeId, muted, prefs.quietHours, java.time.Instant.ofEpochSecond(now), prefs.effectiveZoneId,
                // Doze may defer a quiet-hour broadcast until after quiet hours have ended.
                java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds + offsetSeconds),
            ) && NotificationManagerCompat.from(context).areNotificationsEnabled()) sendSystemNotification(
                id = episode.airingId,
                animeId = episode.animeId,
                title = episode.title,
                episode = episode.episode,
                cover = episode.coverImageUrl?.let { loadBitmap(it) }
            )
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

    private fun sendSystemNotification(id: Int, animeId: Int, title: String, episode: Int, cover: Bitmap?) {
        // Covers the runtime permission on Android 13+ and the app-level toggle on 12/12L, where
        // POST_NOTIFICATIONS does not exist and checking it directly always reads "denied".
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val intent = Intent(
            Intent.ACTION_VIEW,
            "com.owlcoder.animeschedule://detail/$animeId".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notif_content_text, episode))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (cover != null) {
            builder
                .setLargeIcon(cover)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(cover)
                        .bigLargeIcon(null as Bitmap?)
                )
        }

        try {
            notificationManager.notify(id, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked between the check above and posting; the in-app list still has it.
            Log.w(TAG, "Notification permission was revoked while posting", e)
        }
    }

    companion object {
        const val CHANNEL_ID = "airing_episodes"
        private const val TAG = "AiringNotifWorker"
        private const val MAX_ATTEMPTS = 3
        private const val LOOKBACK_SECONDS = 2 * 60 * 60L
        private const val FIRST_RUN_LOOKBACK_SECONDS = 24 * 60 * 60L
    }
}
