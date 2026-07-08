package com.owlcoder.animeschedule.data.api.mal

import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeListResponse
import com.owlcoder.animeschedule.data.api.mal.dto.MalAnimeNode
import com.owlcoder.animeschedule.data.api.mal.dto.MalListStatus
import com.owlcoder.animeschedule.data.api.mal.dto.MalUserResponse

private const val LIST_FIELDS = "id,title,main_picture,num_episodes,my_list_status"
private const val DETAIL_FIELDS = "id,title,main_picture,alternative_titles,start_date,media_type,mean,status,num_episodes,synopsis,genres,my_list_status"

interface MalApiService {
    @GET("v2/users/@me/animelist")
    suspend fun getUserAnimeList(
        @Query("fields") fields: String = LIST_FIELDS,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        // The user's own list must always come back complete — filtering NSFW here made
        // those entries look locally deleted after every full sync.
        @Query("nsfw") nsfw: Boolean = true
    ): MalAnimeListResponse

    @FormUrlEncoded
    @PATCH("v2/anime/{animeId}/my_list_status")
    suspend fun updateListStatus(
        @Path("animeId") animeId: Int,
        @Field("status") status: String? = null,
        @Field("num_watched_episodes") numWatchedEpisodes: Int? = null,
        @Field("score") score: Int? = null
    ): MalListStatus

    @DELETE("v2/anime/{animeId}/my_list_status")
    suspend fun deleteListStatus(@Path("animeId") animeId: Int): Response<Unit>

    @GET("v2/anime/{id}")
    suspend fun getAnimeDetail(
        @Path("id") malId: Int,
        @Query("fields") fields: String = DETAIL_FIELDS
    ): MalAnimeNode

    @GET("v2/users/@me")
    suspend fun getMe(
        @Query("fields") fields: String = "id,name,picture"
    ): MalUserResponse
}
