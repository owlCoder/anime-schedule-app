package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.compositionLocalOf
import com.owlcoder.animeschedule.domain.model.WatchTools

/** Local metadata is available in the same editor from schedule, details and My List. */
data class WatchToolsActions(
    val data: WatchTools = WatchTools(),
    val today: java.time.LocalDate = java.time.LocalDate.now(),
    val toggleFavorite: (Int) -> Unit = {},
    val setNote: (Int, String) -> Unit = { _, _ -> },
    val setTags: (Int, String) -> Unit = { _, _ -> },
    val setEpisodeMinutes: (Int) -> Unit = {},
)

val LocalWatchTools = compositionLocalOf { WatchToolsActions() }
