package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.QuietHours
import com.owlcoder.animeschedule.presentation.components.*
import java.util.Locale

@Composable
internal fun NotificationPreferencesControls(quiet: QuietHours, onQuietChange: (QuietHours) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppChoiceRow(stringResource(R.string.quiet_hours), Icons.Default.Bedtime, quiet.enabled,
            { onQuietChange(quiet.copy(enabled = !quiet.enabled)) }, Modifier.testTag("quiet-hours-enabled"),
            subtitle = stringResource(R.string.quiet_hours_hint), selectionRole = Role.Checkbox)
        if (quiet.enabled) {
            HourAdjustment(stringResource(R.string.quiet_hours_start), quiet.startHour, "quiet-start") { onQuietChange(quiet.copy(startHour = it)) }
            HourAdjustment(stringResource(R.string.quiet_hours_end), quiet.endHour, "quiet-end") { onQuietChange(quiet.copy(endHour = it)) }
            if (quiet.startHour == quiet.endHour) Text(stringResource(R.string.quiet_hours_all_day), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
        val tools = LocalWatchTools.current
        Text(stringResource(R.string.notifications_muted), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        Text(stringResource(R.string.notifications_muted_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        if (tools.data.mutedNotifications.isNotEmpty()) LazyColumn(Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools.data.mutedNotifications.entries.sortedBy { it.value }, key = { it.key }) { (id, name) ->
                val title = name.ifBlank { "Anime #$id" }
                AppButton(stringResource(R.string.notifications_unmute, title), { tools.setNotificationMuted(id, title, false) },
                    Modifier.fillMaxWidth().testTag("unmute-$id"), variant = AppButtonVariant.Secondary, icon = Icons.Default.NotificationsActive)
            }
        }
    }
}

@Composable
private fun HourAdjustment(label: String, hour: Int, tag: String, onChange: (Int) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.ROOT, "%02d:00", hour), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("$tag-value"))
            }
            GlassIconButton(Icons.Default.Remove, stringResource(R.string.quiet_hours_earlier, label), { onChange((hour + 23) % 24) }, Modifier.testTag("$tag-minus"))
            GlassIconButton(Icons.Default.Add, stringResource(R.string.quiet_hours_later, label), { onChange((hour + 1) % 24) }, Modifier.testTag("$tag-plus"))
        }
    }
}
