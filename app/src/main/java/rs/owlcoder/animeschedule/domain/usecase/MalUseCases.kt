package rs.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.domain.model.MalListEntry
import rs.owlcoder.animeschedule.domain.model.MalListUpdate
import rs.owlcoder.animeschedule.domain.repository.MalRepository
import javax.inject.Inject

class GetMalUserListUseCase @Inject constructor(private val malRepository: MalRepository) {
    operator fun invoke(): Flow<AppResult<List<MalListEntry>>> = malRepository.getUserList()
}

class UpdateMalListEntryUseCase @Inject constructor(private val malRepository: MalRepository) {
    suspend operator fun invoke(animeId: Int, update: MalListUpdate): AppResult<Unit> =
        malRepository.updateListEntry(animeId, update)
}

class IncrementEpisodeUseCase @Inject constructor(private val malRepository: MalRepository) {
    suspend operator fun invoke(animeId: Int): AppResult<Unit> =
        malRepository.incrementEpisode(animeId)
}

class RefreshMalListUseCase @Inject constructor(private val malRepository: MalRepository) {
    suspend operator fun invoke() = malRepository.refreshUserList()
}
