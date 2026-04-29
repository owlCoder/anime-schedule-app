package rs.owlcoder.animeschedule.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import rs.owlcoder.animeschedule.data.local.db.AiringEpisodeDao
import rs.owlcoder.animeschedule.data.local.db.AnimeDetailDao
import rs.owlcoder.animeschedule.data.local.db.AnimeScheduleDatabase
import rs.owlcoder.animeschedule.data.local.db.MalListEntryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnimeScheduleDatabase =
        Room.databaseBuilder(context, AnimeScheduleDatabase::class.java, "anime_schedule.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAiringEpisodeDao(db: AnimeScheduleDatabase): AiringEpisodeDao = db.airingEpisodeDao()
    @Provides fun provideAnimeDetailDao(db: AnimeScheduleDatabase): AnimeDetailDao = db.animeDetailDao()
    @Provides fun provideMalListEntryDao(db: AnimeScheduleDatabase): MalListEntryDao = db.malListEntryDao()
}
