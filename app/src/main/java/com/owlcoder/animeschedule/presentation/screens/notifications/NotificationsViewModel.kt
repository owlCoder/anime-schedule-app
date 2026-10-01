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

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(id: Int) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}
