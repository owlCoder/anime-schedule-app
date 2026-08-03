package com.owlcoder.animeschedule.data.api.alternative

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KitsuApiService {
    @GET("anime")
    suspend fun searchAnime(
        @Query("filter[text]") query: String,
        @Query("page[limit]") limit: Int = 20,
        @Query("page[offset]") offset: Int = 0,
        @Query("include") include: String = "mappings"
    ): KitsuAnimeResponse

    @GET("anime")
    suspend fun getSeasonalAnime(
        @Query("filter[season]") season: String,
        @Query("filter[seasonYear]") year: Int,
        @Query("page[limit]") limit: Int = 20,
        @Query("page[offset]") offset: Int = 0
    ): KitsuAnimeResponse

    @GET("anime/{id}")
    suspend fun getAnime(
        @Path("id") id: String,
        @Query("include") include: String = "mappings"
    ): KitsuSingleAnimeResponse
}

@Serializable
data class KitsuAnimeResponse(
    val data: List<KitsuAnimeResource> = emptyList(),
    val included: List<KitsuIncludedResource> = emptyList(),
    val meta: KitsuMeta? = null
)

@Serializable
data class KitsuSingleAnimeResponse(
    val data: KitsuAnimeResource
)

@Serializable
data class KitsuAnimeResource(
    val id: String,
    val attributes: KitsuAnimeAttributes? = null,
    val relationships: KitsuRelationships? = null
)

@Serializable
data class KitsuIncludedResource(
    val id: String,
    val type: String? = null,
    val attributes: JsonObject? = null
)

@Serializable
data class KitsuAnimeAttributes(
    val synopsis: String? = null,
    val description: String? = null,
    val titles: Map<String, String> = emptyMap(),
    val canonicalTitle: String? = null,
    val averageRating: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val nextRelease: String? = null,
    val subtype: String? = null,
    val status: String? = null,
    val episodeCount: Int? = null,
    val episodeLength: Int? = null,
    val posterImage: KitsuImage? = null,
    val coverImage: KitsuImage? = null
)

@Serializable
data class KitsuImage(
    val large: String? = null,
    val medium: String? = null,
    val original: String? = null
)

@Serializable
data class KitsuRelationships(
    val mappings: KitsuMappingRelationship? = null
)

@Serializable
data class KitsuMappingRelationship(
    val data: List<KitsuRelationshipIdentifier> = emptyList()
)

@Serializable
data class KitsuRelationshipIdentifier(
    val id: String,
    val type: String? = null
)

@Serializable
data class KitsuMappingAttributes(
    val externalSite: String? = null,
    val externalId: String? = null
)

@Serializable
data class KitsuMeta(
    val count: Int? = null
)
