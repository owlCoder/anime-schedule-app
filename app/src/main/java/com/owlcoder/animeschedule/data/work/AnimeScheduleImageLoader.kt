package com.owlcoder.animeschedule.data.work

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import okio.Path.Companion.toOkioPath

/** One bounded Coil loader shared by Compose and background workers. */
object AnimeScheduleImageLoader {
    const val MAX_DISK_CACHE_BYTES = 128L * 1024L * 1024L
    private const val DISK_CACHE_DIRECTORY = "anime_schedule_image_cache"

    fun create(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIRECTORY).toOkioPath())
                    .maxSizeBytes(MAX_DISK_CACHE_BYTES)
                    .build()
            }
            .build()
}
