package rs.owlcoder.animeschedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rs.owlcoder.animeschedule.data.local.db.NotificationDao
import rs.owlcoder.animeschedule.data.local.db.NotificationEntity
import rs.owlcoder.animeschedule.data.mapper.toDomain
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.AppNotification
import rs.owlcoder.animeschedule.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

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

    override suspend fun createNotification(episode: AiringEpisode) {
        val now = System.currentTimeMillis() / 1000L
        notificationDao.upsert(
            NotificationEntity(
                id = episode.airingId,
                animeId = episode.animeId,
                title = episode.title,
                episode = episode.episode,
                coverImageUrl = episode.coverImageUrl,
                airingAtEpochSeconds = episode.airingAtEpochSeconds,
                isRead = false,
                createdAtEpochSeconds = now
            )
        )
    }
}
