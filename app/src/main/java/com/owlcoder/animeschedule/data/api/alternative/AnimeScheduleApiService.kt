package com.owlcoder.animeschedule.data.api.alternative

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface AnimeScheduleApiService {
    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): AnimeScheduleAnimePage

    @GET("anime")
    suspend fun getSeasonalAnime(
        @Query("years") year: Int,
        @Query("seasons") season: String,
        @Query("page") page: Int = 1
    ): AnimeScheduleAnimePage

    @GET("anime")
    suspend fun getByAniListId(
        @Query("anilist-ids") anilistId: Int
    ): AnimeScheduleAnimePage

    @GET("anime")
    suspend fun getByMalId(
        @Query("mal-ids") malId: Int
    ): AnimeScheduleAnimePage
}

@Serializable
data class AnimeScheduleAnimePage(
    val page: Int = 1,
    val totalAmount: Int = 0,
    val anime: List<AnimeScheduleAnime> = emptyList()
)

@Serializable
data class AnimeScheduleAnime(
    val id: String,
    val title: String? = null,
    val route: String? = null,
    val description: String? = null,
    val premier: String? = null,
    val season: AnimeScheduleSeason? = null,
    val episodes: Int? = null,
    val lengthMin: Int? = null,
    val status: String? = null,
    val imageVersionRoute: String? = null,
    val genres: List<AnimeScheduleCategory> = emptyList(),
    val mediaTypes: List<AnimeScheduleCategory> = emptyList(),
    val stats: AnimeScheduleStats? = null,
    val names: AnimeScheduleNames? = null,
    val websites: AnimeScheduleWebsites? = null
)

@Serializable
data class AnimeScheduleSeason(
    val title: String? = null,
    val year: String? = null,
    val season: String? = null
)

@Serializable
data class AnimeScheduleCategory(
    val name: String? = null,
    val route: String? = null
)

@Serializable
data class AnimeScheduleStats(
    val averageScore: Double? = null
)

@Serializable
data class AnimeScheduleNames(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val synonyms: List<String> = emptyList()
)

@Serializable
data class AnimeScheduleWebsites(
    val mal: String? = null,
    @SerialName("aniList") val aniList: String? = null,
    val kitsu: String? = null
)
