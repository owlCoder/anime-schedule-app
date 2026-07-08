package com.owlcoder.animeschedule.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A MAL list mutation made while offline (or when the API was down), waiting to be flushed
 * by [com.owlcoder.animeschedule.data.work.PendingUpdatesWorker]. One row per anime — a newer
 * edit merges over / replaces the previous pending one, so the queue always holds the desired
 * end state, not an edit history.
 */
@Entity(tableName = "pending_list_updates")
data class PendingListUpdateEntity(
    @PrimaryKey val animeId: Int,
    val status: String?,
    val numWatchedEpisodes: Int?,
    val score: Int?,
    /** True = the entry should be deleted from the MAL list (wins over field updates). */
    val isRemoval: Boolean,
    val queuedAtEpochMs: Long
)
