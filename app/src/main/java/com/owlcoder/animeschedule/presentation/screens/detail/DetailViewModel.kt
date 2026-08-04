package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.CharacterDetail
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.usecase.GetAnimeDetailUseCase
import com.owlcoder.animeschedule.domain.usecase.GetCharacterDetailUseCase
import com.owlcoder.animeschedule.domain.usecase.GetWatchSourcesUseCase
import com.owlcoder.animeschedule.domain.usecase.IncrementEpisodeUseCase
import com.owlcoder.animeschedule.domain.usecase.RemoveMalListEntryUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val detail: AnimeDetail? = null,
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val watchSources: List<WatchSource> = emptyList(),
    val isIncrementing: Boolean = false,
    @StringRes val errorRes: Int? = null
)

data class CharacterOverlayState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val detail: CharacterDetail? = null,
    val errorRes: Int? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAnimeDetailUseCase: GetAnimeDetailUseCase,
    getWatchSourcesUseCase: GetWatchSourcesUseCase,
    authRepository: AuthRepository,
    private val getCharacterDetailUseCase: GetCharacterDetailUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    private val removeMalListEntryUseCase: RemoveMalListEntryUseCase,
    private val incrementEpisodeUseCase: IncrementEpisodeUseCase
) : ViewModel() {

    private val animeId: Int = checkNotNull(savedStateHandle["animeId"])

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        /** A "+1" tap landed — kept distinct from [Success] (a manual status-sheet save) so
         *  the UI can toast "episode marked" instead of "status saved". */
        data object Incremented : UpdateEvent
        data object Removed : UpdateEvent
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    private val _isIncrementing = MutableStateFlow(false)
    private val reloadSignal = MutableStateFlow(0)

    val uiState: StateFlow<DetailUiState> = reloadSignal
        .flatMapLatest { getAnimeDetailUseCase(animeId) }
        .map { result ->
            when (result) {
                is AppResult.Success -> DetailUiState(detail = result.data, isLoading = false)
                is AppResult.Error -> DetailUiState(isLoading = false, errorRes = R.string.error_load_details)
            }
        }
        .combine(authRepository.isLoggedIn) { state, loggedIn -> state.copy(isLoggedIn = loggedIn) }
        .combine(getWatchSourcesUseCase()) { state, sources -> state.copy(watchSources = sources) }
        .combine(_isIncrementing) { state, incrementing -> state.copy(isIncrementing = incrementing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun reload() {
        reloadSignal.value += 1
    }

    /** Mirrors [ScheduleViewModel.incrementEpisode] — "+1" only ever applies to a WATCHING
     *  entry (gated in the UI), so no DROPPED-specific guard is needed here either. */
    fun incrementEpisode() {
        val malId = uiState.value.detail?.malId ?: uiState.value.detail?.malListEntry?.animeId
        if (malId == null || _isIncrementing.value) return
        _isIncrementing.value = true
        viewModelScope.launch {
            try {
                val result = incrementEpisodeUseCase(malId)
                _updateEvent.send(
                    if (result is AppResult.Success) UpdateEvent.Incremented else UpdateEvent.Error
                )
            } finally {
                _isIncrementing.value = false
            }
        }
    }

    fun updateListEntry(update: MalListUpdate) {
        val malId = uiState.value.detail?.malId ?: uiState.value.detail?.malListEntry?.animeId
        viewModelScope.launch {
            if (malId == null) {
                _updateEvent.send(UpdateEvent.Error)
                return@launch
            }
            val result = updateMalListEntryUseCase(malId, update)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Success else UpdateEvent.Error
            )
        }
    }

    fun removeListEntry() {
        val malId = uiState.value.detail?.malId ?: uiState.value.detail?.malListEntry?.animeId
        viewModelScope.launch {
            if (malId == null) {
                _updateEvent.send(UpdateEvent.Error)
                return@launch
            }
            val result = removeMalListEntryUseCase(malId)
            _updateEvent.send(
                if (result is AppResult.Success) UpdateEvent.Removed else UpdateEvent.Error
            )
        }
    }

    private val _characterOverlay = MutableStateFlow(CharacterOverlayState())
    val characterOverlay: StateFlow<CharacterOverlayState> = _characterOverlay.asStateFlow()

    fun openCharacter(characterId: Int) {
        _characterOverlay.value = CharacterOverlayState(isVisible = true, isLoading = true)
        viewModelScope.launch {
            when (val result = getCharacterDetailUseCase(characterId)) {
                is AppResult.Success -> _characterOverlay.value =
                    CharacterOverlayState(isVisible = true, detail = result.data)
                is AppResult.Error -> _characterOverlay.value =
                    CharacterOverlayState(isVisible = true, errorRes = R.string.error_load_details)
            }
        }
    }

    fun dismissCharacterOverlay() {
        _characterOverlay.value = CharacterOverlayState()
    }
}
