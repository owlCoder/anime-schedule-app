package com.owlcoder.animeschedule.domain.model

import kotlinx.serialization.Serializable

data class MalSyncState(
    val loggedIn: Boolean = false,
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val lastSuccessEpochMs: Long = 0,
    val syncing: Boolean = false,
    val failed: Boolean = false,
    val online: Boolean = true,
)

/** One reversible edit; the repository checks that its resulting values still match. */
data class UndoListChange(
    val id: Long,
    val animeId: Int,
    val title: String,
    val before: MalListUpdate,
    val after: MalListUpdate,
    val sessionEpoch: Long = 0,
)

@Serializable
enum class ToolShortcut { PLANNER, WEEK_OVERVIEW, FAVORITES, HISTORY, CALENDAR, SYNC }

val DefaultToolShortcuts = listOf(ToolShortcut.PLANNER, ToolShortcut.WEEK_OVERVIEW, ToolShortcut.FAVORITES)
fun List<ToolShortcut>.normalizedShortcuts() = distinct().take(4)
