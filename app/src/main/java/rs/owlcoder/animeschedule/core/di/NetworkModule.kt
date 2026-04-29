package rs.owlcoder.animeschedule.core.di

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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
import rs.owlcoder.animeschedule.BuildConfig
import rs.owlcoder.animeschedule.core.network.AuthInterceptor
import rs.owlcoder.animeschedule.core.network.RateLimitInterceptor
import rs.owlcoder.animeschedule.data.api.anilist.buildAniListApolloClient
import rs.owlcoder.animeschedule.data.api.jikan.JikanApiService
import rs.owlcoder.animeschedule.data.api.mal.MalApiService
import rs.owlcoder.animeschedule.data.api.mal.auth.MalAuthService
import rs.owlcoder.animeschedule.data.local.secure.SecureTokenStore
import javax.inject.Named
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

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

    @Provides @Singleton @Named("jikan")
    fun provideJikanRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.jikan.moe/")
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
    fun provideJikanApiService(@Named("jikan") retrofit: Retrofit): JikanApiService =
        retrofit.create(JikanApiService::class.java)
}
