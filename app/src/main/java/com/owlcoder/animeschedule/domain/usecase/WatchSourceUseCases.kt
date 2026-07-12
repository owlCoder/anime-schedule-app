package com.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.repository.WatchSourceRepository
import javax.inject.Inject

class GetWatchSourcesUseCase @Inject constructor(
    private val repo: WatchSourceRepository
) {
    operator fun invoke(): Flow<List<WatchSource>> = repo.getAll()
}

class AddWatchSourceUseCase @Inject constructor(
    private val repo: WatchSourceRepository
) {
    suspend operator fun invoke(name: String, urlTemplate: String, faviconUrl: String?, openExternally: Boolean) =
        repo.add(name, urlTemplate, faviconUrl, openExternally)
}

class UpdateWatchSourceUseCase @Inject constructor(
    private val repo: WatchSourceRepository
) {
    suspend operator fun invoke(source: WatchSource) = repo.update(source)
}

class DeleteWatchSourceUseCase @Inject constructor(
    private val repo: WatchSourceRepository
) {
    suspend operator fun invoke(source: WatchSource) = repo.delete(source)
}
