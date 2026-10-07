package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchStatus

@Composable
fun WatchStatus.displayName(): String = stringResource(labelRes())

fun WatchStatus.labelRes(): Int = when (this) {
    WatchStatus.WATCHING -> R.string.watch_status_watching
    WatchStatus.COMPLETED -> R.string.watch_status_completed
    WatchStatus.ON_HOLD -> R.string.watch_status_on_hold
    WatchStatus.DROPPED -> R.string.watch_status_dropped
    WatchStatus.PLAN_TO_WATCH -> R.string.watch_status_plan_to_watch
    WatchStatus.NOT_IN_LIST -> R.string.watch_status_not_in_list
}
