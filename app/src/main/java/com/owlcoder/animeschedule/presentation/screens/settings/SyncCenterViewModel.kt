package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.*
import com.owlcoder.animeschedule.data.network.NetworkMonitor
import com.owlcoder.animeschedule.domain.model.MalSyncState
import com.owlcoder.animeschedule.domain.repository.MalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SyncCenterViewModel @Inject constructor(private val repository: MalRepository, network: NetworkMonitor) : ViewModel() {
    private val retrying = MutableStateFlow(false)
    private var retryJob: Job? = null
    val state = combine(repository.syncState, network.online, retrying) { state, online, busy ->
        state.copy(online = online, syncing = state.syncing || busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MalSyncState())
    fun retry() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            retrying.value = true
            try { repository.retrySync() } finally { retrying.value = false }
        }
    }
}
