package com.owlcoder.animeschedule.data.work

import android.content.Context
import android.util.Log
import coil3.SingletonImageLoader
import com.owlcoder.animeschedule.data.local.datastore.CacheRetentionPolicy
import com.owlcoder.animeschedule.data.local.datastore.UserPreferencesDataStore
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.NotificationDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheMaintenance @Inject constructor(
    @ApplicationContext private val context: Context,
    private val airingEpisodeDao: AiringEpisodeDao,
    private val animeDetailDao: AnimeDetailDao,
    private val notificationDao: NotificationDao,
    private val userPreferencesDataStore: UserPreferencesDataStore
) {
    /** Runs scheduled cleanup and, when requested, immediately clears image cache as well. */
    suspend fun run(
        nowEpochSeconds: Long = System.currentTimeMillis() / 1000L,
        clearImageCacheNow: Boolean = false
    ) {
        val prefs = userPreferencesDataStore.userPreferencesFlow.first()
        val roomCutoff = CacheRetentionPolicy.cutoffEpochSeconds(
            nowEpochSeconds,
            prefs.cacheRetentionDays
        )

        airingEpisodeDao.deleteAiredBefore(roomCutoff)
        animeDetailDao.deleteStaleUnreferenced(roomCutoff)

        notificationDao.deleteReadOlderThan(
            CacheRetentionPolicy.cutoffEpochSeconds(
                nowEpochSeconds,
                CacheRetentionPolicy.READ_NOTIFICATION_RETENTION_DAYS
            )
        )
        notificationDao.deleteUnreadOlderThan(
            CacheRetentionPolicy.cutoffEpochSeconds(
                nowEpochSeconds,
                CacheRetentionPolicy.UNREAD_NOTIFICATION_RETENTION_DAYS
            )
        )

        val lastImageCacheClear = userPreferencesDataStore.getLastImageCacheClearEpochSeconds()
        val imageCacheClearDue = clearImageCacheNow || lastImageCacheClear == 0L ||
            nowEpochSeconds - lastImageCacheClear >=
            prefs.cacheRetentionDays * CacheRetentionPolicy.SECONDS_PER_DAY

        if (imageCacheClearDue) {
            // Coil's disk cache has a 128 MB hard cap; this periodic clear also enforces age.
            SingletonImageLoader.get(context).diskCache?.clear()
            // Remove the unbounded Coil 3 default cache created by older app versions.
            // The path is app-private cache data and contains no user-owned content.
            context.cacheDir.resolve(LEGACY_COIL_CACHE_DIRECTORY).deleteRecursively()
            userPreferencesDataStore.setLastImageCacheClearEpochSeconds(nowEpochSeconds)
        }

        Log.d(TAG, "Cache maintenance completed (retention=${prefs.cacheRetentionDays}d)")
    }

    private companion object {
        const val TAG = "CacheMaintenance"
        const val LEGACY_COIL_CACHE_DIRECTORY = "image_cache"
    }
}
