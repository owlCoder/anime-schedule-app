package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.compositionLocalOf
import com.owlcoder.animeschedule.domain.model.MalSyncState
import java.time.ZoneId

data class SyncCenterActions(val state: MalSyncState = MalSyncState(), val zone: ZoneId = ZoneId.systemDefault(),
    val retry: () -> Unit = {}, val login: () -> Unit = {})
val LocalSyncCenter = compositionLocalOf { SyncCenterActions() }
