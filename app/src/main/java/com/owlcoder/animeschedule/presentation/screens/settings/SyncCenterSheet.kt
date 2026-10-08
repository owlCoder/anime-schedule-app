package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalSyncState
import com.owlcoder.animeschedule.presentation.components.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.*

@Composable
internal fun syncHeadline(state: MalSyncState): String = when {
    !state.loggedIn -> stringResource(R.string.sync_sign_in)
    state.syncing -> stringResource(R.string.sync_in_progress)
    state.rejectedCount > 0 -> stringResource(R.string.sync_needs_attention)
    state.pendingCount > 0 -> pluralStringResource(if (state.online) R.plurals.sync_pending else R.plurals.sync_pending_offline, state.pendingCount, state.pendingCount)
    state.failed -> stringResource(R.string.sync_failed)
    else -> stringResource(R.string.sync_all_saved)
}

@Composable
internal fun SyncSummary(state: MalSyncState, onOpen: () -> Unit) {
    AppActionRow(syncHeadline(state), syncIcon(state), onOpen, Modifier.fillMaxWidth().testTag("sync-summary"), subtitle = stringResource(R.string.sync_center))
}

private fun syncIcon(state: MalSyncState): ImageVector = when {
        !state.loggedIn -> Icons.Default.AccountCircle
        state.syncing -> Icons.Default.Sync
        state.rejectedCount > 0 || state.failed -> Icons.Default.SyncProblem
        state.pendingCount > 0 -> Icons.Default.CloudUpload
        else -> Icons.Default.CloudDone
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SyncCenterSheet(state: MalSyncState, onRetry: () -> Unit, onLogin: () -> Unit, onDismiss: () -> Unit, zone: ZoneId = ZoneId.systemDefault()) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.sync_center)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(syncIcon(state), null, tint = if (state.loggedIn && (state.rejectedCount > 0 || state.failed)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text(syncHeadline(state), Modifier.testTag("sync-status"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (state.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(if (state.online) R.string.sync_online else R.string.sync_offline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(stringResource(if (!state.loggedIn && state.pendingCount > 0) R.string.sync_session_expired else if (state.rejectedCount > 0) R.string.sync_rejected_hint else if (state.pendingCount > 0) R.string.sync_queue_hint else R.string.sync_saved_hint), style = MaterialTheme.typography.bodyMedium)
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sync_last_success), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val locale = LocalConfiguration.current.locales[0]
                    val lastSuccess = remember(state.lastSuccessEpochMs, locale, zone) {
                        if (state.lastSuccessEpochMs <= 0) null else DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale).withZone(zone).format(Instant.ofEpochMilli(state.lastSuccessEpochMs))
                    }
                    Text(lastSuccess ?: stringResource(R.string.sync_never), Modifier.testTag("sync-last-success"), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (state.failed && state.rejectedCount == 0) Text(stringResource(R.string.sync_retry_hint), color = MaterialTheme.colorScheme.error)
            if (!state.loggedIn) AppButton(stringResource(R.string.profile_login), onLogin, Modifier.fillMaxWidth().testTag("sync-login"), icon = Icons.AutoMirrored.Filled.Login)
            else AppButton(stringResource(R.string.sync_retry), onRetry, Modifier.fillMaxWidth().testTag("sync-retry"), enabled = !state.syncing && state.online, icon = Icons.Default.Refresh)
        }
    }
}
