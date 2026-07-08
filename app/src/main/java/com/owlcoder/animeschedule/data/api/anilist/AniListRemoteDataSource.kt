package com.owlcoder.animeschedule.data.api.anilist

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.ApolloException
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.anilist.generated.AiringScheduleQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailByMalQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.AnimeSearchQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.SeasonalAnimeQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSeason
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AniListRemoteDataSource @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getAiringSchedule(
        from: Long,
        to: Long
    ): AppResult<List<AiringScheduleQuery.AiringSchedule>> {
        return try {
            val allResults = mutableListOf<AiringScheduleQuery.AiringSchedule>()
            var page = 1
            var hasNextPage = true
            while (hasNextPage) {
                val response = apolloClient.query(
                    AiringScheduleQuery(
                        page = com.apollographql.apollo.api.Optional.present(page),
                        perPage = com.apollographql.apollo.api.Optional.present(50),
                        airingAt_greater = com.apollographql.apollo.api.Optional.present(from.toInt()),
                        airingAt_lesser = com.apollographql.apollo.api.Optional.present(to.toInt())
                    )
                ).execute()
                if (response.hasErrors()) {
                    return AppResult.Error(AppError.GraphQL(response.errors!!.first().message))
                }
                val pageData = response.data?.Page ?: break
                pageData.airingSchedules?.filterNotNull()?.let { allResults.addAll(it) }
                hasNextPage = pageData.pageInfo?.hasNextPage == true
                page++
            }
            AppResult.Success(allResults)
        } catch (e: ApolloException) {
            AppResult.Error(AppError.Network(e.message))
        }
    }

    suspend fun getAnimeDetail(id: Int): AppResult<AnimeDetailQuery.Media> {
        return try {
            val response = apolloClient.query(
                AnimeDetailQuery(id = com.apollographql.apollo.api.Optional.present(id))
            ).execute()
            if (response.hasErrors()) {
                return AppResult.Error(AppError.GraphQL(response.errors!!.first().message))
            }
            val media = response.data?.Media
                ?: return AppResult.Error(AppError.NoCache)
            AppResult.Success(media)
        } catch (e: ApolloException) {
            AppResult.Error(AppError.Network(e.message))
        }
    }

    data class SearchPageResult(
        val media: List<AnimeSearchQuery.Medium>,
        val hasNextPage: Boolean
    )

    suspend fun searchAnime(
        query: String,
        page: Int = 1,
        perPage: Int = 20
    ): AppResult<SearchPageResult> {
        return try {
            val response = apolloClient.query(
                AnimeSearchQuery(
                    search = com.apollographql.apollo.api.Optional.present(query),
                    page = com.apollographql.apollo.api.Optional.present(page),
                    perPage = com.apollographql.apollo.api.Optional.present(perPage)
                )
            ).execute()
            if (response.hasErrors()) {
                return AppResult.Error(AppError.GraphQL(response.errors!!.first().message))
            }
            val pageData = response.data?.Page
            AppResult.Success(
                SearchPageResult(
                    media = pageData?.media?.filterNotNull() ?: emptyList(),
                    hasNextPage = pageData?.pageInfo?.hasNextPage == true
                )
            )
        } catch (e: ApolloException) {
            AppResult.Error(AppError.Network(e.message))
        }
    }

    suspend fun getSeasonalAnime(
        season: MediaSeason,
        year: Int,
        sort: MediaSort = MediaSort.POPULARITY_DESC,
        page: Int = 1,
        perPage: Int = 50
    ): AppResult<List<SeasonalAnimeQuery.Medium>> {
        return try {
            val allResults = mutableListOf<SeasonalAnimeQuery.Medium>()
            var currentPage = page
            var hasNextPage = true
            while (hasNextPage) {
                val response = apolloClient.query(
                    SeasonalAnimeQuery(
                        season = com.apollographql.apollo.api.Optional.present(season),
                        seasonYear = com.apollographql.apollo.api.Optional.present(year),
                        page = com.apollographql.apollo.api.Optional.present(currentPage),
                        perPage = com.apollographql.apollo.api.Optional.present(perPage),
                        sort = com.apollographql.apollo.api.Optional.present(listOf(sort))
                    )
                ).execute()
                if (response.hasErrors()) {
                    return AppResult.Error(AppError.GraphQL(response.errors!!.first().message))
                }
                val pageData = response.data?.Page ?: break
                pageData.media?.filterNotNull()?.let { allResults.addAll(it) }
                hasNextPage = pageData.pageInfo?.hasNextPage == true && currentPage < 4
                currentPage++
            }
            AppResult.Success(allResults)
        } catch (e: ApolloException) {
            AppResult.Error(AppError.Network(e.message))
        }
    }

    suspend fun getAnimeDetailByMalId(malId: Int): AppResult<AnimeDetailByMalQuery.Media> {
        return try {
            val response = apolloClient.query(
                AnimeDetailByMalQuery(idMal = com.apollographql.apollo.api.Optional.present(malId))
            ).execute()
            if (response.hasErrors()) {
                return AppResult.Error(AppError.GraphQL(response.errors!!.first().message))
            }
            val media = response.data?.Media
                ?: return AppResult.Error(AppError.NoCache)
            AppResult.Success(media)
        } catch (e: ApolloException) {
            AppResult.Error(AppError.Network(e.message))
        }
    }
}
