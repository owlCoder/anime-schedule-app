package com.owlcoder.animeschedule.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.owlcoder.animeschedule.data.repository.AnimeDetailRepositoryImpl
import com.owlcoder.animeschedule.data.repository.AuthRepositoryImpl
import com.owlcoder.animeschedule.data.repository.MalRepositoryImpl
import com.owlcoder.animeschedule.data.repository.NotificationRepositoryImpl
import com.owlcoder.animeschedule.data.repository.ScheduleRepositoryImpl
import com.owlcoder.animeschedule.data.repository.SearchRepositoryImpl
import com.owlcoder.animeschedule.data.repository.SeasonalRepositoryImpl
import com.owlcoder.animeschedule.data.repository.SettingsRepositoryImpl
import com.owlcoder.animeschedule.data.repository.WatchSourceRepositoryImpl
import com.owlcoder.animeschedule.data.work.PendingUpdateScheduler
import com.owlcoder.animeschedule.data.work.WorkManagerPendingUpdateScheduler
import com.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.repository.MalRepository
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import com.owlcoder.animeschedule.domain.repository.ScheduleRepository
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import com.owlcoder.animeschedule.domain.repository.SettingsRepository
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
    @Binds @Singleton abstract fun bindAnimeDetailRepository(impl: AnimeDetailRepositoryImpl): AnimeDetailRepository
    @Binds @Singleton abstract fun bindMalRepository(impl: MalRepositoryImpl): MalRepository
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindSeasonalRepository(impl: SeasonalRepositoryImpl): SeasonalRepository
    @Binds @Singleton abstract fun bindPendingUpdateScheduler(impl: WorkManagerPendingUpdateScheduler): PendingUpdateScheduler
    @Binds @Singleton abstract fun bindWatchSourceRepository(impl: WatchSourceRepositoryImpl): WatchSourceRepository
}
