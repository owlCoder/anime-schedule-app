package com.owlcoder.animeschedule.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import com.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import com.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.local.db.NotificationDao
import com.owlcoder.animeschedule.data.local.db.PendingListUpdateDao
import com.owlcoder.animeschedule.data.local.db.WatchSourceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE pending_list_updates ADD COLUMN rejected INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS notification_actions (notificationId INTEGER NOT NULL, malId INTEGER NOT NULL, progressHandled INTEGER NOT NULL, snoozeGeneration INTEGER NOT NULL, snoozedUntilEpochMs INTEGER, PRIMARY KEY(notificationId))")
        }
    }
    val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE notification_actions ADD COLUMN actionToken TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE notification_actions SET actionToken = lower(hex(randomblob(16)))")
        }
    }
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnimeScheduleDatabase =
        Room.databaseBuilder(context, AnimeScheduleDatabase::class.java, "anime_schedule.db")
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
            // Schemas 1-7 shipped without exported migrations, so those installs can only be reset.
            // From version 8 on a missing migration fails loudly instead of silently wiping the
            // offline MAL edit queue, notifications and watch sources.
            .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 7)
            .build()

    @Provides fun provideAiringEpisodeDao(db: AnimeScheduleDatabase): AiringEpisodeDao = db.airingEpisodeDao()
    @Provides fun provideAnimeDetailDao(db: AnimeScheduleDatabase): AnimeDetailDao = db.animeDetailDao()
    @Provides fun provideMalListEntryDao(db: AnimeScheduleDatabase): MalListEntryDao = db.malListEntryDao()
    @Provides fun provideNotificationDao(db: AnimeScheduleDatabase): NotificationDao = db.notificationDao()
    @Provides fun providePendingListUpdateDao(db: AnimeScheduleDatabase): PendingListUpdateDao = db.pendingListUpdateDao()
    @Provides fun provideWatchSourceDao(db: AnimeScheduleDatabase): WatchSourceDao = db.watchSourceDao()
    @Provides fun provideNotificationActionDao(db: AnimeScheduleDatabase): com.owlcoder.animeschedule.data.local.db.NotificationActionDao = db.notificationActionDao()
}
