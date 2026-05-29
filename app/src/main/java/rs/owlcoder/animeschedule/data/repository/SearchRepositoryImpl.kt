package rs.owlcoder.animeschedule.data.repository

import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import rs.owlcoder.animeschedule.data.mapper.toSearchResult
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult
import rs.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val aniListDataSource: AniListRemoteDataSource
) : SearchRepository {

    override suspend fun searchAnime(query: String, page: Int): AppResult<List<AnimeSearchResult>> {
        if (query.length < 2) return AppResult.Success(emptyList())
        return when (val result = aniListDataSource.searchAnime(query = query, page = page + 1)) {
            is AppResult.Success -> AppResult.Success(result.data.map { it.toSearchResult() })
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }
}
