package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.usecase.GetNotificationsUseCase
import com.owlcoder.animeschedule.domain.usecase.MarkAllNotificationsReadUseCase
import com.owlcoder.animeschedule.domain.usecase.MarkNotificationReadUseCase
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    getNotificationsUseCase: GetNotificationsUseCase,
    private val markReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllNotificationsReadUseCase
) : ViewModel() {

    val notifications: StateFlow<List<AppNotification>> = getNotificationsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markRead(id: Int) {
        viewModelScope.launch { markReadUseCase(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { markAllReadUseCase() }
    }
}
