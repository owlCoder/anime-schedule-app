package rs.owlcoder.animeschedule.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import rs.owlcoder.animeschedule.data.repository.AnimeDetailRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.AuthRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.MalRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.NotificationRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.ScheduleRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.SearchRepositoryImpl
import rs.owlcoder.animeschedule.data.repository.SettingsRepositoryImpl
import rs.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import rs.owlcoder.animeschedule.domain.repository.AuthRepository
import rs.owlcoder.animeschedule.domain.repository.MalRepository
import rs.owlcoder.animeschedule.domain.repository.NotificationRepository
import rs.owlcoder.animeschedule.domain.repository.ScheduleRepository
import rs.owlcoder.animeschedule.domain.repository.SearchRepository
import rs.owlcoder.animeschedule.domain.repository.SettingsRepository
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
}
