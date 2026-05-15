package rs.owlcoder.animeschedule.data.api.anilist

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.exception.ApolloException
import rs.owlcoder.animeschedule.core.result.AppError
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.anilist.generated.AiringScheduleQuery
import rs.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailByMalQuery
import rs.owlcoder.animeschedule.data.api.anilist.generated.AnimeDetailQuery
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
