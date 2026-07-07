package com.owlcoder.animeschedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import javax.inject.Inject

class GetNotificationsUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    operator fun invoke(): Flow<List<AppNotification>> = repo.getAll()
}

class GetUnreadCountUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    operator fun invoke(): Flow<Int> = repo.getUnreadCount()
}

class MarkNotificationReadUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    suspend operator fun invoke(id: Int) = repo.markRead(id)
}

class MarkAllNotificationsReadUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    suspend operator fun invoke() = repo.markAllRead()
}
