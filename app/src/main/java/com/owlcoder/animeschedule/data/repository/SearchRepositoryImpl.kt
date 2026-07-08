package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.first
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.data.api.anilist.AniListRemoteDataSource
import com.owlcoder.animeschedule.data.local.db.MalListEntryDao
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toSearchResult
import com.owlcoder.animeschedule.domain.model.SearchPage
import com.owlcoder.animeschedule.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val aniListDataSource: AniListRemoteDataSource,
    private val malListEntryDao: MalListEntryDao
) : SearchRepository {

    override suspend fun searchAnime(query: String, page: Int): AppResult<SearchPage> {
        if (query.length < 2) return AppResult.Success(SearchPage(emptyList(), hasNextPage = false))
        val malEntries = malListEntryDao.getAll().first()
        val malByMalId = malEntries.associateBy { it.malId }
        return when (val result = aniListDataSource.searchAnime(query = query, page = page + 1)) {
            is AppResult.Success -> AppResult.Success(
                SearchPage(
                    results = result.data.media.map { medium ->
                        val entry = medium.idMal?.let { malByMalId[it]?.toDomain() }
                        medium.toSearchResult(existingEntry = entry)
                    },
                    hasNextPage = result.data.hasNextPage
                )
            )
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }
}
