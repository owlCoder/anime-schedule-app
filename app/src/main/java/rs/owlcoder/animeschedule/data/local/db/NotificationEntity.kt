package rs.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    val animeId: Int,
    val title: String,
    val episode: Int,
    val coverImageUrl: String?,
    val airingAtEpochSeconds: Long,
    val isRead: Boolean = false,
    val createdAtEpochSeconds: Long
)
