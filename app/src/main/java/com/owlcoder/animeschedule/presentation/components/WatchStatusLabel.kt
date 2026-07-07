package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchStatus

@Composable
fun WatchStatus.displayName(): String = when (this) {
    WatchStatus.WATCHING -> stringResource(R.string.watch_status_watching)
    WatchStatus.COMPLETED -> stringResource(R.string.watch_status_completed)
    WatchStatus.ON_HOLD -> stringResource(R.string.watch_status_on_hold)
    WatchStatus.DROPPED -> stringResource(R.string.watch_status_dropped)
    WatchStatus.PLAN_TO_WATCH -> stringResource(R.string.watch_status_plan_to_watch)
    WatchStatus.NOT_IN_LIST -> stringResource(R.string.watch_status_not_in_list)
}
