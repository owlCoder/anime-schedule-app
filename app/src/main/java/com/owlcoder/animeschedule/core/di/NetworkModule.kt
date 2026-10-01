package com.owlcoder.animeschedule.core.di

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.core.network.AuthInterceptor
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
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType

private const val CALL_TIMEOUT_SECONDS = 30L

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideProviderClock(clock: SystemProviderClock): ProviderClock = clock

    @Provides @Singleton
    fun provideOkHttpClient(secureTokenStore: SecureTokenStore): OkHttpClient =
        OkHttpClient.Builder()
            // Bounds the whole exchange (DNS, connect, TLS, body) so a stalled provider can
            // never hold a request open indefinitely.
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor { secureTokenStore.getMalAccessToken() })
            .addInterceptor(
                HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }.apply {
                    // Never log bodies: the OAuth token endpoint returns access/refresh tokens.
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                    redactHeader("Authorization")
                }
            )
            .build()

    @Provides @Singleton
    fun provideApolloClient(okHttpClient: OkHttpClient): ApolloClient =
        buildAniListApolloClient(okHttpClient)

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
