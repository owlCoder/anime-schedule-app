package com.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import com.owlcoder.animeschedule.data.local.db.NotificationDao
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import com.owlcoder.animeschedule.data.mapper.toDomain

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getAll(): Flow<List<AppNotification>> =
        notificationDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getUnreadCount(): Flow<Int> =
        notificationDao.getUnreadCount()

    override suspend fun markRead(id: Int) =
        notificationDao.markRead(id)

    override suspend fun markAllRead() =
        notificationDao.markAllRead()

    override suspend fun deleteRead() = notificationDao.deleteRead()
}
