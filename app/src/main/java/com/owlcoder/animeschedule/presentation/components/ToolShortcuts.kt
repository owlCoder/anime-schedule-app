package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*

fun ToolShortcut.labelRes(): Int = when (this) {
    ToolShortcut.PLANNER -> R.string.watch_planner
    ToolShortcut.WEEK_OVERVIEW -> R.string.schedule_agenda
    ToolShortcut.FAVORITES -> R.string.favorites
    ToolShortcut.HISTORY -> R.string.watch_history
    ToolShortcut.CALENDAR -> R.string.activity_calendar
    ToolShortcut.SYNC -> R.string.sync_center
}
fun ToolShortcut.icon(): ImageVector = when (this) {
    ToolShortcut.PLANNER -> Icons.Default.Timer
    ToolShortcut.WEEK_OVERVIEW -> Icons.Default.DateRange
    ToolShortcut.FAVORITES -> Icons.Default.Star
    ToolShortcut.HISTORY -> Icons.Default.History
    ToolShortcut.CALENDAR -> Icons.Default.CalendarMonth
    ToolShortcut.SYNC -> Icons.Default.CloudSync
}
private fun ToolShortcut.shortLabelRes(): Int = when (this) {
    ToolShortcut.PLANNER -> R.string.shortcut_planner_label
    ToolShortcut.HISTORY -> R.string.shortcut_history_label
    ToolShortcut.CALENDAR -> R.string.shortcut_calendar_label
    ToolShortcut.WEEK_OVERVIEW -> R.string.shortcut_week_label
    ToolShortcut.SYNC -> R.string.shortcut_sync_label
    else -> labelRes()
}

@Composable
fun ToolShortcutBar(values: List<ToolShortcut>, onOpen: (ToolShortcut) -> Unit, onCustomize: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("tool-shortcuts"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.shortcuts_title), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GlassIconButton(Icons.Default.Tune, stringResource(R.string.shortcuts_customize), onCustomize, Modifier.testTag("shortcut-customize"))
        }
        values.normalizedShortcuts().chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { value ->
                    val fullLabel = stringResource(value.labelRes())
                    AppButton(stringResource(value.shortLabelRes()), { onOpen(value) },
                        Modifier.weight(1f).fillMaxHeight().testTag("shortcut-${value.name}").semantics { contentDescription = fullLabel },
                        variant = AppButtonVariant.Secondary, icon = value.icon())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolShortcutsSheet(values: List<ToolShortcut>, onChange: (List<ToolShortcut>) -> Unit, onDismiss: () -> Unit) {
    val selected = values.normalizedShortcuts()
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.shortcuts_title)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.shortcuts_hint), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.shortcuts_selected, selected.size), Modifier.testTag("shortcut-selection-count"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            (selected + ToolShortcut.entries.filterNot { it in selected }).forEach { value ->
                val active = value in selected
                val enabled = active || selected.size < 4
                val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                val label = stringResource(value.labelRes())
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(if (active) 1.dp else .5.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().toggleable(active, enabled = enabled, role = Role.Checkbox) { checked -> onChange(if (checked) selected + value else selected - value) }
                        .testTag("shortcut-choice-${value.name}").padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(value.icon(), null, Modifier.size(24.dp), tint = if (enabled) MaterialTheme.colorScheme.primary else tint)
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = tint)
                        if (active) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            val index = selected.indexOf(value)
                            fun move(offset: Int) { val next = selected.toMutableList(); next[index] = next[index + offset]; next[index + offset] = value; onChange(next) }
                            IconButton({ move(-1) }, Modifier.testTag("shortcut-up-${value.name}"), enabled = index > 0) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.shortcuts_up, label)) }
                            IconButton({ move(1) }, Modifier.testTag("shortcut-down-${value.name}"), enabled = index < selected.lastIndex) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.shortcuts_down, label)) }
                        } else Icon(Icons.Default.Add, null, Modifier.padding(12.dp), tint = tint)
                    }
                }
            }
            AppButton(stringResource(R.string.common_reset), { onChange(DefaultToolShortcuts) }, Modifier.fillMaxWidth(), variant = AppButtonVariant.Secondary, icon = Icons.Default.RestartAlt)
        }
    }
}
