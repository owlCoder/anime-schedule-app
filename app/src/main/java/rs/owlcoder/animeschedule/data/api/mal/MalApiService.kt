package rs.owlcoder.animeschedule.data.api.mal

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import rs.owlcoder.animeschedule.data.api.mal.dto.MalAnimeListResponse
import rs.owlcoder.animeschedule.data.api.mal.dto.MalAnimeNode
import rs.owlcoder.animeschedule.data.api.mal.dto.MalListStatus
import rs.owlcoder.animeschedule.data.api.mal.dto.MalSearchResponse
import rs.owlcoder.animeschedule.data.api.mal.dto.MalUserResponse

private const val LIST_FIELDS = "id,title,main_picture,num_episodes,my_list_status"
private const val SEARCH_FIELDS = "id,title,main_picture,alternative_titles,start_date,media_type,mean,my_list_status,num_episodes"
private const val DETAIL_FIELDS = "id,title,main_picture,alternative_titles,start_date,media_type,mean,status,num_episodes,synopsis,genres,my_list_status"

interface MalApiService {
    @GET("v2/users/@me/animelist")
    suspend fun getUserAnimeList(
        @Query("fields") fields: String = LIST_FIELDS,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = false
    ): MalAnimeListResponse

    @FormUrlEncoded
    @PATCH("v2/anime/{animeId}/my_list_status")
    suspend fun updateListStatus(
        @Path("animeId") animeId: Int,
        @Field("status") status: String? = null,
        @Field("num_watched_episodes") numWatchedEpisodes: Int? = null,
        @Field("score") score: Int? = null
    ): MalListStatus

    @GET("v2/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = SEARCH_FIELDS,
        @Query("nsfw") nsfw: Boolean = false
    ): MalSearchResponse

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
