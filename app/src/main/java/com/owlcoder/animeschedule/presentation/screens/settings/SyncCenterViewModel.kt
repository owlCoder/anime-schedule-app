package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.data.network.NetworkMonitor
import com.owlcoder.animeschedule.domain.model.MalSyncState
import com.owlcoder.animeschedule.domain.repository.MalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncCenterViewModel @Inject constructor(
    private val repository: MalRepository,
    network: NetworkMonitor,
) : ViewModel() {
    private val retrying = MutableStateFlow(false)
    private val retryFailedAt = MutableStateFlow<Long?>(null)
    private var retryJob: Job? = null
    private val repositoryState = repository.syncState.onEach {
        // A transient retry failure belongs to this session, not the next account.
        if (!it.loggedIn) retryFailedAt.value = null
    }
    val state = combine(repositoryState, network.online, retrying, retryFailedAt) { state, online, busy, failedAt ->
        state.copy(
            online = online,
            syncing = state.syncing || busy,
            failed = state.failed || (failedAt != null && state.loggedIn && state.lastSuccessEpochMs == failedAt),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MalSyncState())

    fun retry() {
        if (retryJob?.isActive == true) return
        retryJob = viewModelScope.launch {
            retrying.value = true
            retryFailedAt.value = null
            val lastSuccess = state.value.lastSuccessEpochMs
            try {
                if (!repository.retrySync()) retryFailedAt.value = lastSuccess
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                retryFailedAt.value = lastSuccess
            } finally {
                retrying.value = false
            }
        }
    }
}
