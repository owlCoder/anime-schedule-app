package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.repository.AuthRepository
import com.owlcoder.animeschedule.domain.usecase.GetAnimeDetailUseCase
import com.owlcoder.animeschedule.domain.usecase.RemoveMalListEntryUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class DetailUiState(
    val detail: AnimeDetail? = null,
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    @StringRes val errorRes: Int? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getAnimeDetailUseCase: GetAnimeDetailUseCase,
    authRepository: AuthRepository,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase,
    private val removeMalListEntryUseCase: RemoveMalListEntryUseCase
) : ViewModel() {

    private val animeId: Int = checkNotNull(savedStateHandle["animeId"])

    sealed interface UpdateEvent {
        data object Success : UpdateEvent
        data object Removed : UpdateEvent
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val uiState: StateFlow<DetailUiState> = getAnimeDetailUseCase(animeId)
        .map { result ->
            when (result) {
                is AppResult.Success -> DetailUiState(detail = result.data, isLoading = false)
                is AppResult.Error -> DetailUiState(isLoading = false, errorRes = R.string.error_load_details)
            }
        }
        .combine(authRepository.isLoggedIn) { state, loggedIn -> state.copy(isLoggedIn = loggedIn) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun updateListEntry(update: MalListUpdate) {
        val malId = uiState.value.detail?.malId ?: uiState.value.detail?.malListEntry?.animeId
        viewModelScope.launch {
            // No MAL mapping for this AniList entry — surface the failure instead of
            // silently dropping the user's edit.
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
}
