package com.owlcoder.animeschedule.domain.usecase

import com.owlcoder.animeschedule.domain.model.AnimeSeason
import com.owlcoder.animeschedule.domain.repository.SeasonalRepository
import javax.inject.Inject

class GetSeasonalAnimeUseCase @Inject constructor(
    private val repository: SeasonalRepository
) {
    suspend operator fun invoke(season: AnimeSeason, year: Int) =
        repository.getSeasonalAnime(season, year)
}
