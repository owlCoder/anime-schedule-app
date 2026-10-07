package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    private val _clearing = MutableStateFlow(false)
    val clearing = _clearing.asStateFlow()
    private val _clearError = MutableStateFlow(false)
    val clearError = _clearError.asStateFlow()
    private val _marking = MutableStateFlow(false)
    val marking = _marking.asStateFlow()
    private val _markError = MutableStateFlow(false)
    val markError = _markError.asStateFlow()

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(id: Int) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        mark { notificationRepository.markAllRead() }
    }

    fun setRead(id: Int, read: Boolean) = mark { notificationRepository.setRead(id, read) }

    fun markVisibleRead(ids: List<Int>) {
        if (ids.isEmpty()) return
        val snapshot = ids.distinct()
        mark { notificationRepository.markRead(snapshot) }
    }

    private fun mark(action: suspend () -> Unit) {
        if (_marking.value) return
        _marking.value = true
        _markError.value = false
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _markError.value = true }
            finally { _marking.value = false }
        }
    }

    fun clearRead() {
        if (_clearing.value) return
        _clearing.value = true
        _clearError.value = false
        viewModelScope.launch {
            try {
                notificationRepository.deleteRead()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _clearError.value = true
            } finally {
                _clearing.value = false
            }
        }
    }
}
