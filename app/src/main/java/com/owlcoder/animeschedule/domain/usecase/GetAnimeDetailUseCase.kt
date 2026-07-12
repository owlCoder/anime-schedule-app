package com.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.CharacterDetail
import com.owlcoder.animeschedule.domain.repository.AnimeDetailRepository
import javax.inject.Inject

class GetAnimeDetailUseCase @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository
) {
    operator fun invoke(animeId: Int): Flow<AppResult<AnimeDetail>> =
        animeDetailRepository.getAnimeDetail(animeId)
}

class GetCharacterDetailUseCase @Inject constructor(
    private val animeDetailRepository: AnimeDetailRepository
) {
    suspend operator fun invoke(characterId: Int): AppResult<CharacterDetail> =
        animeDetailRepository.getCharacterDetail(characterId)
}
