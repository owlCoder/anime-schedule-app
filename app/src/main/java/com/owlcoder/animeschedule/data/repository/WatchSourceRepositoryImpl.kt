package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.owlcoder.animeschedule.data.local.db.WatchSourceDao
import com.owlcoder.animeschedule.data.local.db.WatchSourceEntity
import com.owlcoder.animeschedule.data.mapper.toDomain
import com.owlcoder.animeschedule.data.mapper.toEntity
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import javax.inject.Inject
import javax.inject.Singleton

private fun faviconUrl(domain: String) = "https://www.google.com/s2/favicons?domain=$domain&sz=64"

/** Legal, licensed streaming services offered as a starting point — users can add any
 *  other source themselves from Settings. */
private val defaultSources = listOf(
    WatchSourceEntity(
        name = "Crunchyroll",
        urlTemplate = "https://www.crunchyroll.com/search?q={query}",
        faviconUrl = faviconUrl("crunchyroll.com"),
        sortOrder = 0,
        openExternally = true
    ),
    WatchSourceEntity(
        name = "Netflix",
        urlTemplate = "https://www.netflix.com/search?q={query}",
        faviconUrl = faviconUrl("netflix.com"),
        sortOrder = 1,
        openExternally = true
    )
)

@Singleton
class WatchSourceRepositoryImpl @Inject constructor(
    private val dao: WatchSourceDao
) : WatchSourceRepository {

    override fun getAll(): Flow<List<WatchSource>> =
        dao.getAll()
            .onStart { seedDefaultsIfEmpty() }
            .map { list -> list.map { it.toDomain() } }

    private suspend fun seedDefaultsIfEmpty() {
        if (dao.nextSortOrder() == 0) {
            defaultSources.forEach { dao.insert(it) }
        }
    }

    override suspend fun add(name: String, urlTemplate: String, faviconUrl: String?, openExternally: Boolean) {
        dao.insert(
            WatchSourceEntity(
                name = name,
                urlTemplate = urlTemplate,
                faviconUrl = faviconUrl,
                sortOrder = dao.nextSortOrder(),
                openExternally = openExternally
            )
        )
    }

    override suspend fun update(source: WatchSource) {
        dao.update(source.toEntity())
    }

    override suspend fun delete(source: WatchSource) {
        dao.delete(source.toEntity())
    }
}
