@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import kotlin.math.roundToInt

@Composable
internal fun RatingRangeSheet(current: Pair<Int, Int>, onApply: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var range by remember { mutableStateOf(current.first.toFloat()..current.second.toFloat()) }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.rating_filter)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.rating_filter_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            WorkspaceCard {
                Text(stringResource(R.string.rating_value, range.start.roundToInt(), range.endInclusive.roundToInt()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                RangeSlider(range, { range = it }, Modifier.fillMaxWidth().testTag("rating-range"), valueRange = 0f..10f, steps = 9)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("0", style = MaterialTheme.typography.labelMedium); Text("10", style = MaterialTheme.typography.labelMedium) }
            }
            AppButton(stringResource(R.string.rating_apply), { onApply(range.start.roundToInt(), range.endInclusive.roundToInt()) }, Modifier.fillMaxWidth().testTag("rating-apply"), icon = Icons.Default.Check)
            AppButton(stringResource(R.string.rating_reset), { onApply(0, 10) }, Modifier.fillMaxWidth(), variant = AppButtonVariant.Secondary, icon = Icons.Default.FilterAltOff)
        }
    }
}

@Composable
internal fun BulkOrganizeSheet(entries: List<MalListEntry>, onApply: (Set<Int>, Boolean?, Boolean?) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = entries.filter { it.title.contains(query.trim(), true) }
    val current = selected.intersect(entries.map { it.animeId }.toSet())
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.bulk_tools)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).testTag("bulk-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.bulk_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                WorkspaceCard {
                    Text(stringResource(R.string.bulk_selected, current.size), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    EqualActions {
                        AppButton(stringResource(R.string.bulk_all), { selected = selected + visible.map { it.animeId } }, Modifier.weight(1f).fillMaxHeight().testTag("bulk-all"), variant = AppButtonVariant.Secondary, icon = Icons.Default.SelectAll)
                        AppButton(stringResource(R.string.bulk_none), { selected = emptySet() }, Modifier.weight(1f).fillMaxHeight(), variant = AppButtonVariant.Secondary, icon = Icons.Default.Deselect)
                    }
                    listOf(Triple(R.string.bulk_favorite, true, null), Triple(R.string.bulk_unfavorite, false, null), Triple(R.string.bulk_pin, null, true), Triple(R.string.bulk_unpin, null, false)).forEachIndexed { index, (label, favorite, pin) ->
                        AppButton(stringResource(label), { onApply(current, favorite, pin) }, Modifier.fillMaxWidth().testTag("bulk-action-$index"), enabled = current.isNotEmpty(), variant = if (index == 0 || index == 2) AppButtonVariant.Primary else AppButtonVariant.Secondary, icon = if (favorite != null) Icons.Default.Star else Icons.Default.PushPin)
                    }
                }
            }
            item { AppSearchField(query, { query = it }, placeholder = stringResource(R.string.workspace_title_search), leadingIcon = Icons.Default.Search, onClear = { query = "" }) }
            items(visible, key = { it.animeId }) { entry ->
                AppChoiceRow(entry.title.ifBlank { "#${entry.animeId}" }, Icons.Default.Checklist, entry.animeId in current, { selected = if (entry.animeId in selected) selected - entry.animeId else selected + entry.animeId }, Modifier.testTag("bulk-${entry.animeId}"), subtitle = entry.status.displayName(), selectionRole = Role.Checkbox)
            }
        }
    }
}

@Composable
internal fun ManageTagsSheet(tools: WatchTools, onRename: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var replacement by rememberSaveable { mutableStateOf("") }
    val tags = tools.tags.values.flatten().distinctBy { it.lowercase(java.util.Locale.ROOT) }.sortedBy { it.lowercase(java.util.Locale.ROOT) }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.manage_tags)) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.manage_tags_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (tags.isEmpty()) item { EmptyState(Icons.Default.LabelOff, stringResource(R.string.tags_empty)) }
            items(tags, key = { it }) { tag ->
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Label, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(tag, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.tag_usage, tools.tags.values.count { labels -> labels.any { it.equals(tag, true) } }), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ editing = tag; deleting = null; replacement = tag }, Modifier.testTag("tag-edit-$tag")) { Icon(Icons.Default.Edit, stringResource(R.string.rename_tag, tag)) }
                        IconButton({ deleting = tag; editing = null; focus.clearFocus(); keyboard?.hide() }) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.delete_tag, tag)) }
                    }
                    if (editing == tag) {
                        OutlinedTextField(replacement, { replacement = it.take(24) }, Modifier.fillMaxWidth().testTag("tag-replacement"), singleLine = true, label = { Text(stringResource(R.string.new_tag_name)) }, shape = MaterialTheme.shapes.large, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }))
                        EqualActions {
                            AppButton(stringResource(R.string.common_cancel), { editing = null; focus.clearFocus(); keyboard?.hide() }, Modifier.weight(1f).fillMaxHeight(), variant = AppButtonVariant.Secondary, icon = Icons.Default.Close)
                            AppButton(stringResource(R.string.common_save), { onRename(tag, replacement.trim()); editing = null; focus.clearFocus(); keyboard?.hide() }, Modifier.weight(1f).fillMaxHeight().testTag("tag-rename-save"), enabled = replacement.isNotBlank() && ',' !in replacement, icon = Icons.Default.Check)
                        }
                    }
                    if (deleting == tag) {
                        Text(stringResource(R.string.tag_delete_confirm, tag), style = MaterialTheme.typography.bodyMedium)
                        EqualActions {
                            AppButton(stringResource(R.string.common_cancel), { deleting = null }, Modifier.weight(1f).fillMaxHeight(), variant = AppButtonVariant.Secondary, icon = Icons.Default.Close)
                            AppButton(stringResource(R.string.tag_remove_action), { onRename(tag, null); deleting = null }, Modifier.weight(1f).fillMaxHeight().testTag("tag-delete-confirm"), variant = AppButtonVariant.Destructive, icon = Icons.Default.DeleteOutline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BacklogSheet(entries: List<MalListEntry>, tools: WatchTools, onDismiss: () -> Unit) {
    val groups = remember(entries, tools) { entries.backlog(tools) }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.backlog_dashboard)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.backlog_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(groups, key = { it.status }) { group ->
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.HourglassTop, null, tint = MaterialTheme.colorScheme.primary)
                        Text(group.status.displayName(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(group.titles.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.backlog_episodes_time, group.episodes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), group.episodes, group.minutes / 60, group.minutes % 60), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    if (group.unknown > 0) Text(stringResource(R.string.backlog_unknown, group.unknown), style = MaterialTheme.typography.bodySmall)
                    group.daysAt(tools.dailyGoal)?.takeIf { group.episodes > 0 }?.let { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.backlog_days_estimate, it.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), it, tools.dailyGoal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (tools.dailyGoal == 0) item { Text(stringResource(R.string.backlog_goal_off), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
internal fun CompareAnimeSheet(entries: List<MalListEntry>, tools: WatchTools, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(emptyList<Int>()) }
    var query by rememberSaveable { mutableStateOf("") }
    val chosen = selected.mapNotNull { id -> entries.find { it.animeId == id } }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.compare_anime)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).testTag("compare-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.compare_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (chosen.size == 2) item {
                WorkspaceCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { chosen.forEach { Text(it.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } }
                    HorizontalDivider()
                    CompareRow(stringResource(R.string.detail_status), chosen.map { it.status.displayName() })
                    CompareRow(stringResource(R.string.compare_rating), chosen.map { if (it.score > 0) "${it.score}/10" else stringResource(R.string.compare_unrated) })
                    CompareRow(stringResource(R.string.compare_progress), chosen.map { "${it.episodesWatched}/${it.totalEpisodes?.takeIf { total -> total > 0 } ?: "?"}" })
                    CompareRow(stringResource(R.string.compare_duration), chosen.map { tools.minutesFor(it.animeId).toString() })
                    CompareRow(stringResource(R.string.compare_remaining), chosen.map { entry -> entry.remainingMinutes(tools)?.let { stringResource(R.string.compare_time, it / 60, it % 60) } ?: "—" })
                }
            }
            item {
                Text(stringResource(R.string.compare_selected, chosen.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                AppSearchField(query, { query = it }, placeholder = stringResource(R.string.workspace_title_search), leadingIcon = Icons.Default.Search, onClear = { query = "" })
            }
            items(entries.filter { it.title.contains(query.trim(), true) }, key = { it.animeId }) { entry ->
                AppChoiceRow(entry.title, Icons.Default.CompareArrows, entry.animeId in selected, { selected = if (entry.animeId in selected) selected - entry.animeId else if (selected.size < 2) selected + entry.animeId else listOf(selected.last(), entry.animeId) }, Modifier.testTag("compare-${entry.animeId}"), subtitle = entry.status.displayName(), selectionRole = Role.Checkbox)
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, values: List<String>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { values.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) } }
    }
}

@Composable
internal fun WorkspaceCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun EqualActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
