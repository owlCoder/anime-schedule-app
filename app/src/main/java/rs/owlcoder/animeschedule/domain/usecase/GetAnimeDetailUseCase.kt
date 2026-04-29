package rs.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import rs.owlcoder.animeschedule.core.result.AppResult
import rs.owlcoder.animeschedule.domain.model.AnimeDetail
import rs.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import javax.inject.Inject

class GetAnimeDetailUseCase @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository
) {
    operator fun invoke(animeId: Int): Flow<AppResult<AnimeDetail>> =
        animeDetailRepository.getAnimeDetail(animeId)
}
