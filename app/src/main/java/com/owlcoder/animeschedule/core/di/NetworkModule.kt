package com.owlcoder.animeschedule.core.di

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.core.network.AuthInterceptor
import com.owlcoder.animeschedule.core.network.RateLimitInterceptor
import com.owlcoder.animeschedule.data.api.alternative.AnimeScheduleApiService
import com.owlcoder.animeschedule.data.api.alternative.KitsuApiService
import com.owlcoder.animeschedule.data.api.anilist.buildAniListApolloClient
import com.owlcoder.animeschedule.data.api.mal.MalApiService
import com.owlcoder.animeschedule.data.api.mal.auth.MalAuthService
import com.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import com.owlcoder.animeschedule.data.provider.ProviderClock
import com.owlcoder.animeschedule.data.provider.SystemProviderClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideProviderClock(clock: SystemProviderClock): ProviderClock = clock

    @Provides @Singleton
    fun provideOkHttpClient(secureTokenStore: SecureTokenStore): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { secureTokenStore.getMalAccessToken() })
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor(HttpLoggingInterceptor { msg ->
                android.util.Log.d("OkHttp", msg)
            }.apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .build()

    @Provides @Singleton
    fun provideApolloClient(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ApolloClient = buildAniListApolloClient(context, okHttpClient)

    @Provides @Singleton @Named("mal")
    fun provideMalRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.myanimelist.net/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton @Named("malAuth")
    fun provideMalAuthRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://myanimelist.net/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton @Named("kitsu")
    fun provideKitsuRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://kitsu.io/api/edge/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/vnd.api+json".toMediaType()))
        .build()

    @Provides @Singleton @Named("animeSchedule")
    fun provideAnimeScheduleRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://animeschedule.net/api/v3/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideMalApiService(@Named("mal") retrofit: Retrofit): MalApiService =
        retrofit.create(MalApiService::class.java)

    @Provides @Singleton
    fun provideMalAuthService(@Named("malAuth") retrofit: Retrofit): MalAuthService =
        retrofit.create(MalAuthService::class.java)

    @Provides @Singleton
    fun provideKitsuApiService(@Named("kitsu") retrofit: Retrofit): KitsuApiService =
        retrofit.create(KitsuApiService::class.java)

    @Provides @Singleton
    fun provideAnimeScheduleApiService(@Named("animeSchedule") retrofit: Retrofit): AnimeScheduleApiService =
        retrofit.create(AnimeScheduleApiService::class.java)
}
