package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.core.result.AppResult
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.domain.usecase.GetAnimeDetailUseCase
import com.owlcoder.animeschedule.domain.usecase.UpdateMalListEntryUseCase
import javax.inject.Inject

data class DetailUiState(
    val detail: AnimeDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getAnimeDetailUseCase: GetAnimeDetailUseCase,
    private val updateMalListEntryUseCase: UpdateMalListEntryUseCase
) : ViewModel() {

    private val animeId: Int = checkNotNull(savedStateHandle["animeId"])

    sealed interface UpdateEvent {
        data object Error : UpdateEvent
    }
    private val _updateEvent = Channel<UpdateEvent>(Channel.BUFFERED)
    val updateEvent = _updateEvent.receiveAsFlow()

    val uiState: StateFlow<DetailUiState> = getAnimeDetailUseCase(animeId)
        .map { result ->
            when (result) {
                is AppResult.Success -> DetailUiState(detail = result.data, isLoading = false)
                is AppResult.Error -> DetailUiState(isLoading = false, error = "Greška pri učitavanju detalja")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun updateListEntry(update: MalListUpdate) {
        val malId = uiState.value.detail?.malId ?: uiState.value.detail?.malListEntry?.animeId ?: return
        viewModelScope.launch {
            val result = updateMalListEntryUseCase(malId, update)
            if (result is AppResult.Error) {
                _updateEvent.send(UpdateEvent.Error)
            }
        }
    }
}
