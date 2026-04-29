package rs.owlcoder.animeschedule.data.repository

import rs.owlcoder.animeschedule.core.result.AppError
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.mal.MalApiService
import rs.owlcoder.animeschedule.data.mapper.toSearchResult
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult
import rs.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val malApiService: MalApiService
) : SearchRepository {

    override suspend fun searchAnime(query: String, page: Int): AppResult<List<AnimeSearchResult>> {
        if (query.length < 2) return AppResult.Success(emptyList())
        return try {
            val response = malApiService.searchAnime(query = query, offset = page * 20)
            AppResult.Success(response.data.map { it.node.toSearchResult() })
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> AppResult.Error(AppError.Unauthorized)
                429 -> AppResult.Error(AppError.RateLimit())
                else -> AppResult.Error(AppError.Network(e.message()))
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Network(e.message))
        }
    }
}
