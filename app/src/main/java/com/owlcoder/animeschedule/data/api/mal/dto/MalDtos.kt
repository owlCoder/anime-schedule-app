package com.owlcoder.animeschedule.data.api.mal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String
)

@Serializable
data class MalAnimeListResponse(
    val data: List<MalAnimeListItem>,
    val paging: MalPaging? = null
)

@Serializable
data class MalAnimeListItem(
    val node: MalAnimeNode
)

@Serializable
data class MalAnimeNode(
    val id: Int,
    val title: String,
    @SerialName("main_picture") val mainPicture: MalPicture? = null,
    @SerialName("alternative_titles") val alternativeTitles: MalAlternativeTitles? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    val mean: Double? = null,
    val status: String? = null,
    @SerialName("num_episodes") val numEpisodes: Int? = null,
    @SerialName("my_list_status") val myListStatus: MalListStatus? = null
)

@Serializable
data class MalPicture(
    val medium: String? = null,
    val large: String? = null
)

@Serializable
data class MalAlternativeTitles(
    val en: String? = null,
    val ja: String? = null
)

@Serializable
data class MalListStatus(
    val status: String,
    @SerialName("num_episodes_watched") val numEpisodesWatched: Int = 0,
    val score: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MalPaging(
    val next: String? = null,
    val previous: String? = null
)

@Serializable
data class MalUserResponse(
    val id: Int,
    val name: String,
    val picture: String? = null
)
