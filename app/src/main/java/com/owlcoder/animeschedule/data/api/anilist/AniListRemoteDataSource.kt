package com.owlcoder.animeschedule.data.api.anilist

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloHttpException
import com.owlcoder.animeschedule.core.result.AppError
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.anilist.generated.AiringScheduleQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.AnimeSearchQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.CharacterDetailQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.SeasonalAnimeQuery
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSeason
import com.owlcoder.animeschedule.data.api.anilist.generated.type.MediaSort
import javax.inject.Inject
import javax.inject.Singleton
import com.apollographql.apollo.exception.ApolloException

@Singleton
class AniListRemoteDataSource @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getAiringSchedule(
        from: Long,
        to: Long
    ): AppResult<List<AiringScheduleQuery.AiringSchedule>> = fetchAllPages(
        maxPages = AIRING_SCHEDULE_MAX_PAGES,
        query = { page ->
            AiringScheduleQuery(
                page = Optional.present(page),
                perPage = Optional.present(AIRING_SCHEDULE_PAGE_SIZE),
                airingAt_greater = Optional.present(from.toInt()),
                airingAt_lesser = Optional.present(to.toInt())
            )
        },
        items = { it.Page?.airingSchedules?.filterNotNull().orEmpty() },
        hasNextPage = { it.Page?.pageInfo?.hasNextPage == true }
    )

    suspend fun getAnimeDetail(id: Int): AppResult<AnimeDetailQuery.Media> =
        fetchDetail(AnimeDetailQuery(id = Optional.present(id))) { it.Media }

    suspend fun getAnimeDetailByMalId(malId: Int): AppResult<AnimeDetailQuery.Media> =
        fetchDetail(AnimeDetailQuery(idMal = Optional.present(malId))) { it.Media }

    data class SearchPageResult(
        val media: List<AnimeSearchQuery.Medium>,
        val hasNextPage: Boolean
    )

    suspend fun searchAnime(
        query: String,
        page: Int = 1,
        perPage: Int = SEARCH_PAGE_SIZE
    ): AppResult<SearchPageResult> {
        val request = AnimeSearchQuery(
            search = Optional.present(query),
            page = Optional.present(page),
            perPage = Optional.present(perPage)
        )
        return execute(request).mapData { data ->
            SearchPageResult(
                media = data.Page?.media?.filterNotNull().orEmpty(),
                hasNextPage = data.Page?.pageInfo?.hasNextPage == true
            )
        }
    }

    suspend fun getSeasonalAnime(
        season: MediaSeason,
        year: Int,
        sort: MediaSort = MediaSort.POPULARITY_DESC,
        perPage: Int = SEASONAL_PAGE_SIZE
    ): AppResult<List<SeasonalAnimeQuery.Medium>> = fetchAllPages(
        maxPages = SEASONAL_MAX_PAGES,
        query = { page ->
            SeasonalAnimeQuery(
                season = Optional.present(season),
                seasonYear = Optional.present(year),
                page = Optional.present(page),
                perPage = Optional.present(perPage),
                sort = Optional.present(listOf(sort))
            )
        },
        items = { it.Page?.media?.filterNotNull().orEmpty() },
        hasNextPage = { it.Page?.pageInfo?.hasNextPage == true }
    )

    suspend fun getCharacterDetail(id: Int): AppResult<CharacterDetailQuery.Character> =
        execute(CharacterDetailQuery(id = Optional.present(id)))
            .mapData { it.Character ?: return AppResult.Error(AppError.NoCache) }

    private suspend fun <D : Query.Data, T : Any> fetchDetail(
        query: Query<D>,
        selector: (D) -> T?
    ): AppResult<T> = execute(query).mapData { selector(it) ?: return AppResult.Error(AppError.NoCache) }

    /**
     * Fetches consecutive pages until the server reports no further page or [maxPages] is hit.
     * A failure on any page fails the whole call so callers never cache a truncated result.
     */
    private suspend fun <D : Query.Data, T> fetchAllPages(
        maxPages: Int,
        query: (page: Int) -> Query<D>,
        items: (D) -> List<T>,
        hasNextPage: (D) -> Boolean
    ): AppResult<List<T>> {
        val all = mutableListOf<T>()
        for (page in 1..maxPages) {
            val data = when (val result = execute(query(page))) {
                is AppResult.Success -> result.data
                is AppResult.Error -> return result
            }
            all += items(data)
            if (!hasNextPage(data)) break
        }
        return AppResult.Success(all)
    }

    private suspend fun <D : Query.Data> execute(query: Query<D>): AppResult<D> =
        apolloClient.query(query).execute().toAppResult()

    private inline fun <D, R> AppResult<D>.mapData(transform: (D) -> R): AppResult<R> = when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
    }

    private companion object {
        const val AIRING_SCHEDULE_PAGE_SIZE = 50
        const val AIRING_SCHEDULE_MAX_PAGES = 20
        const val SEARCH_PAGE_SIZE = 20
        const val SEASONAL_PAGE_SIZE = 50
        const val SEASONAL_MAX_PAGES = 4
    }
}

/**
 * Apollo 4 does not throw for transport failures: network, HTTP and parse problems arrive in
 * [ApolloResponse.exception] with null data. Treating that as "no data" would make an offline
 * request look like a successful empty page and silence the provider fallback chain.
 */
private fun <D : Operation.Data> ApolloResponse<D>.toAppResult(): AppResult<D> {
    exception?.let { return AppResult.Error(it.toAppError()) }
    errors?.firstOrNull()?.let { return AppResult.Error(AppError.GraphQL(it.message)) }
    return data?.let { AppResult.Success(it) }
        ?: AppResult.Error(AppError.Unknown("Empty GraphQL response"))
}

private fun com.apollographql.apollo.exception.ApolloException.toAppError(): AppError =
    if (this is ApolloHttpException) {
        if (statusCode == HTTP_TOO_MANY_REQUESTS) {
            val retryAfter = headers
                .firstOrNull { it.name.equals("Retry-After", ignoreCase = true) }
                ?.value?.toLongOrNull()
            retryAfter?.let { AppError.RateLimit(it) } ?: AppError.RateLimit()
        } else {
            AppError.Network("HTTP $statusCode", statusCode)
        }
    } else {
        AppError.Network(message)
    }

private const val HTTP_TOO_MANY_REQUESTS = 429
