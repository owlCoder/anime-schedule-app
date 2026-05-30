package rs.owlcoder.animeschedule.domain.usecase

import rs.owlcoder.animeschedule.domain.model.AnimeSeason
import rs.owlcoder.animeschedule.domain.repository.SeasonalRepository
import javax.inject.Inject

class GetSeasonalAnimeUseCase @Inject constructor(
    private val repository: SeasonalRepository
) {
    suspend operator fun invoke(season: AnimeSeason, year: Int) =
        repository.getSeasonalAnime(season, year)
}
