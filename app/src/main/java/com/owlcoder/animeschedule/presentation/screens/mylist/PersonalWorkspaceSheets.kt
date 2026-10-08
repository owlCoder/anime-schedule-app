@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun SmartListFilter.labelRes() = when (this) {
    SmartListFilter.ALL -> R.string.smart_all
    SmartListFilter.PINNED -> R.string.pinned_anime
    SmartListFilter.SHORT_SERIES -> R.string.smart_short
    SmartListFilter.NEAR_FINISH -> R.string.smart_near_finish
    SmartListFilter.UNSTARTED -> R.string.smart_unstarted
    SmartListFilter.WITH_NOTES -> R.string.smart_with_notes
    SmartListFilter.UNTAGGED -> R.string.smart_untagged
    SmartListFilter.LONG_SERIES -> R.string.smart_long_series
    SmartListFilter.UNKNOWN_LENGTH -> R.string.smart_unknown_length
    SmartListFilter.IN_PROGRESS -> R.string.smart_in_progress
    SmartListFilter.COMPLETED_UNRATED -> R.string.smart_completed_unrated
}

private fun SmartListFilter.icon() = when (this) {
    SmartListFilter.ALL -> Icons.Default.FilterAltOff
    SmartListFilter.PINNED -> Icons.Default.PushPin
    SmartListFilter.SHORT_SERIES -> Icons.Default.Timer
    SmartListFilter.NEAR_FINISH -> Icons.Default.Flag
    SmartListFilter.UNSTARTED -> Icons.Default.NewReleases
    SmartListFilter.WITH_NOTES -> Icons.Default.Notes
    SmartListFilter.UNTAGGED -> Icons.Default.LabelOff
    SmartListFilter.LONG_SERIES -> Icons.Default.PlaylistPlay
    SmartListFilter.UNKNOWN_LENGTH -> Icons.Default.HelpOutline
    SmartListFilter.IN_PROGRESS -> Icons.Default.PlayCircle
    SmartListFilter.COMPLETED_UNRATED -> Icons.Default.StarOutline
}

@Composable
internal fun SmartFiltersSheet(current: SmartListFilter, onSelect: (SmartListFilter) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.smart_filters)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 590.dp).testTag("smart-filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(stringResource(R.string.smart_filters_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(SmartListFilter.entries, key = { it.name }) { filter ->
                AppChoiceRow(stringResource(filter.labelRes()), filter.icon(), current == filter, { onSelect(filter) }, Modifier.testTag("smart-${filter.name}"))
            }
        }
    }
}

@Composable
internal fun SavedViewsSheet(views: List<SavedListView>, onSave: (String) -> Unit, onApply: (SavedListView) -> Unit, onDelete: (String) -> Unit, onDismiss: () -> Unit,
    onRename: (String, String) -> Unit = { _, _ -> }, onMove: (String, Int) -> Unit = { _, _ -> }) {
    var name by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.saved_list_views)) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.saved_views_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                OutlinedTextField(name, { name = it.take(32) }, Modifier.fillMaxWidth().testTag("view-name"), singleLine = true,
                    label = { Text(stringResource(R.string.view_name)) }, leadingIcon = { Icon(Icons.Default.Edit, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); keyboard?.hide() }), shape = MaterialTheme.shapes.large)
            }
            item {
                AppButton(stringResource(R.string.view_save), { onSave(name.trim()); name = ""; focus.clearFocus(); keyboard?.hide() },
                    Modifier.fillMaxWidth().testTag("view-save"), enabled = name.isNotBlank(), icon = Icons.Default.BookmarkAdd)
            }
            if (views.isEmpty()) item { EmptyState(Icons.Default.Bookmarks, stringResource(R.string.views_empty), modifier = Modifier.padding(vertical = 16.dp)) }
            items(views, key = { it.name }) { view ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppButton(view.name, { onApply(view) }, Modifier.weight(1f).testTag("saved-view-${view.name}"), variant = AppButtonVariant.Plain, icon = Icons.Default.Bookmark)
                            IconButton({ onDelete(view.name) }) { Icon(Icons.Default.DeleteOutline, stringResource(R.string.view_delete, view.name)) }
                        }
                        val status = view.status?.displayName() ?: stringResource(R.string.list_all_statuses)
                        Text("$status · ${stringResource(view.smartFilter.labelRes())}", Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (view.minimumScore != 0 || view.maximumScore != 10) Text(stringResource(R.string.rating_value, view.minimumScore, view.maximumScore), Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (view.query.isNotBlank()) Text(view.query, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton({ editing = view.name; draft = view.name }, Modifier.testTag("view-rename-${view.name}")) { Icon(Icons.Default.Edit, stringResource(R.string.view_rename, view.name)) }
                            val index = views.indexOf(view)
                            IconButton({ onMove(view.name, -1) }, Modifier.testTag("view-up-${view.name}"), enabled = index > 0) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.shortcuts_up, view.name)) }
                            IconButton({ onMove(view.name, 1) }, Modifier.testTag("view-down-${view.name}"), enabled = index < views.lastIndex) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.shortcuts_down, view.name)) }
                        }
                        if (editing == view.name) {
                            val duplicate = views.any { it.name != view.name && it.name.equals(draft.trim(), true) }
                            OutlinedTextField(draft, { draft = it.take(32) }, Modifier.fillMaxWidth().testTag("view-rename-field"), singleLine = true, label = { Text(stringResource(R.string.view_name)) },
                                isError = duplicate, supportingText = { if (duplicate) Text(stringResource(R.string.view_duplicate)) }, shape = MaterialTheme.shapes.large)
                            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(stringResource(R.string.common_cancel), { editing = null; focus.clearFocus(); keyboard?.hide() }, Modifier.weight(1f).fillMaxHeight(), variant = AppButtonVariant.Plain, icon = Icons.Default.Close)
                                AppButton(stringResource(R.string.common_save), { onRename(view.name, draft); editing = null; focus.clearFocus(); keyboard?.hide() }, Modifier.weight(1f).fillMaxHeight().testTag("view-rename-save"), enabled = draft.isNotBlank() && !duplicate, icon = Icons.Default.Check)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WatchPlannerSheet(entries: List<MalListEntry>, tools: WatchTools, onOpen: (Int) -> Unit, onDismiss: () -> Unit) {
    var budget by rememberSaveable { mutableIntStateOf(60) }
    var strategy by remember { mutableStateOf(PlannerStrategy.BALANCED) }
    var excluded by remember { mutableStateOf(emptySet<Int>()) }
    var showStyle by remember { mutableStateOf(false) }
    var showTitles by remember { mutableStateOf(false) }
    var includePaused by rememberSaveable { mutableStateOf(false) }
    var includePlanned by rememberSaveable { mutableStateOf(false) }
    var pause by rememberSaveable { mutableIntStateOf(0) }
    val statuses = setOf(WatchStatus.WATCHING) + (if (includePaused) setOf(WatchStatus.ON_HOLD) else emptySet()) + (if (includePlanned) setOf(WatchStatus.PLAN_TO_WATCH) else emptySet())
    val candidates = remember(entries, statuses) { entries.filter { it.animeId > 0 && it.status in statuses && (it.totalEpisodes == null || it.totalEpisodes <= 0 || it.episodesWatched < it.totalEpisodes) }.distinctBy { it.animeId } }
    val plan = remember(entries, tools, budget, strategy, excluded, statuses, pause) { planWatchSession(entries.sortedFor(MyListSortOrder.RECENT), tools, budget, strategy, excluded, statuses, pause) }
    val rest = (plan.sumOf { it.episodes } - 1).coerceAtLeast(0) * pause
    val used = plan.sumOf { it.minutes } + rest
    val context = androidx.compose.ui.platform.LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.watch_planner)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).testTag("planner-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.planner_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { NumberSetting(stringResource(R.string.planner_budget), budget, 15, 480, 15, { budget = it }, "planner") }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 120).forEach { value -> AppButton(stringResource(R.string.episode_length_value, value), { budget = value }, Modifier.weight(1f).fillMaxHeight().testTag("planner-budget-$value"), variant = AppButtonVariant.Secondary, icon = Icons.Default.Timer) }
                }
            }
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.planner_episode_count, plan.sumOf { it.episodes }, plan.sumOf { it.episodes }, used), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.planner_unused, budget - used), style = MaterialTheme.typography.bodySmall)
                        if (rest > 0) Text(stringResource(R.string.planner_rest_total, rest), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("planner-rest-total"))
                    }
                }
            }
            if (plan.isEmpty()) item { EmptyState(Icons.Default.Schedule, stringResource(R.string.planner_empty), stringResource(R.string.planner_empty_hint)) }
            items(plan, key = { it.entry.animeId }) { item ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AppButton(item.entry.title.ifBlank { "#${item.entry.animeId}" }, { onOpen(item.entry.animeId) }, Modifier.fillMaxWidth().testTag("plan-${item.entry.animeId}"), variant = AppButtonVariant.Plain, icon = Icons.Default.PlayArrow)
                        Text(androidx.compose.ui.res.pluralStringResource(R.plurals.planner_episode_count, item.episodes, item.episodes, item.minutes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(planEpisodeRange(resources, item), style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("plan-range-${item.entry.animeId}"))
                    }
                }
            }
            item {
                AppButton(stringResource(R.string.planner_strategy) + " · " + stringResource(when (strategy) { PlannerStrategy.BALANCED -> R.string.planner_balanced; PlannerStrategy.FINISH_FIRST -> R.string.planner_finish_first; PlannerStrategy.FOCUS -> R.string.planner_focus }), { showStyle = !showStyle }, Modifier.fillMaxWidth().testTag("planner-style"), variant = AppButtonVariant.Secondary, icon = Icons.Default.Tune)
            }
            if (showStyle) items(PlannerStrategy.entries, key = { "style-$it" }) { option ->
                val label = when (option) { PlannerStrategy.BALANCED -> R.string.planner_balanced; PlannerStrategy.FINISH_FIRST -> R.string.planner_finish_first; PlannerStrategy.FOCUS -> R.string.planner_focus }
                val hint = when (option) { PlannerStrategy.BALANCED -> R.string.planner_balanced_hint; PlannerStrategy.FINISH_FIRST -> R.string.planner_finish_first_hint; PlannerStrategy.FOCUS -> R.string.planner_focus_hint }
                AppChoiceRow(stringResource(label), Icons.Default.Tune, strategy == option, { strategy = option; showStyle = false }, Modifier.testTag("planner-style-${option.name}"), subtitle = stringResource(hint))
            }
            item {
                AppButton(stringResource(R.string.planner_choose), { showTitles = !showTitles }, Modifier.fillMaxWidth().testTag("planner-choose"), variant = AppButtonVariant.Secondary, icon = Icons.Default.Checklist)
                Text(stringResource(R.string.planner_candidates, candidates.count { it.animeId !in excluded }, candidates.size), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showTitles) {
                item { Text(stringResource(R.string.planner_selection_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(candidates, key = { "candidate-${it.animeId}" }) { entry ->
                    AppChoiceRow(entry.title, Icons.Default.PlayCircle, entry.animeId !in excluded, { excluded = if (entry.animeId in excluded) excluded - entry.animeId else excluded + entry.animeId }, Modifier.testTag("planner-include-${entry.animeId}"), selectionRole = Role.Checkbox)
                }
            }
            item { AppChoiceRow(stringResource(R.string.planner_include_paused), Icons.Default.PauseCircle, includePaused, { includePaused = !includePaused }, Modifier.testTag("planner-paused"), selectionRole = Role.Checkbox) }
            item { AppChoiceRow(stringResource(R.string.planner_include_planned), Icons.Default.BookmarkAdd, includePlanned, { includePlanned = !includePlanned }, Modifier.testTag("planner-planned"), selectionRole = Role.Checkbox) }
            item { NumberSetting(stringResource(R.string.planner_break), pause, 0, 30, 5, { pause = it }, "planner-break") }
            item { Text(stringResource(R.string.planner_break_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { AppButton(stringResource(R.string.planner_share), {
                val text = buildString {
                    append(resources.getString(R.string.watch_planner)); append(" · "); append(resources.getString(R.string.episode_length_value, used))
                    plan.forEach { item -> append("\n"); append(item.entry.title); append(" · "); append(planEpisodeRange(resources, item)) }
                    if (rest > 0) { append("\n"); append(resources.getString(R.string.planner_rest_total, rest)) }
                }
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(android.content.Intent.EXTRA_TEXT, text) }
                context.startActivity(android.content.Intent.createChooser(intent, resources.getString(R.string.planner_share)))
            }, Modifier.fillMaxWidth().testTag("planner-share"), enabled = plan.isNotEmpty(), variant = AppButtonVariant.Secondary, icon = Icons.Default.Share) }
        }
    }
}

private fun planEpisodeRange(resources: android.content.res.Resources, item: WatchPlanItem): String {
    val first = item.entry.episodesWatched.coerceAtLeast(0).toLong() + 1
    return if (item.episodes == 1) resources.getString(R.string.planner_single_episode, first)
        else resources.getString(R.string.planner_episode_range, first, first + item.episodes - 1)
}

@Composable
internal fun ActivityCalendarSheet(tools: WatchTools, onDailyGoal: (Int) -> Unit, onDismiss: () -> Unit) {
    val today = LocalWatchTools.current.today
    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    val selected = runCatching { LocalDate.parse(selectedIso) }.getOrDefault(today)
    val counts = remember(tools.activity, today) { tools.dailyEpisodes(today) }
    val streak = remember(tools.activity, today) { tools.streak(today) }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val days = remember(today) { (27L downTo 0L).map { today.minusDays(it) } }
    val padding = days.first().dayOfWeek.value - 1
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.activity_calendar)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).testTag("activity-calendar-list"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalendarMetric(stringResource(R.string.current_streak), androidx.compose.ui.res.pluralStringResource(R.plurals.streak_day_count, streak.current, streak.current), Modifier.weight(1f), Icons.Default.LocalFireDepartment)
                    CalendarMetric(stringResource(R.string.best_streak), androidx.compose.ui.res.pluralStringResource(R.plurals.streak_day_count, streak.best, streak.best), Modifier.weight(1f), Icons.Default.EmojiEvents)
                }
            }
            item {
                NumberSetting(stringResource(R.string.daily_goal), tools.dailyGoal, 0, 50, 1, onDailyGoal, "daily-goal")
                val count = counts[today] ?: 0
                Text(if (tools.dailyGoal == 0) stringResource(R.string.daily_goal_off, count) else stringResource(R.string.daily_goal_progress, count, tools.dailyGoal), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            item { Text(stringResource(R.string.calendar_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                val cells = List<LocalDate?>(padding) { null } + days
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val rangeFormatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM uuuu", locale) }
                    Text(stringResource(R.string.calendar_range, days.first().format(rangeFormatter), today.format(rangeFormatter)), Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth()) {
                        (1..7).forEach { number -> Text(java.time.DayOfWeek.of(number).getDisplayName(java.time.format.TextStyle.NARROW, locale), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { date ->
                                if (date == null) Spacer(Modifier.weight(1f)) else {
                                    val count = counts[date] ?: 0
                                    val active = date == selected
                                    val description = stringResource(R.string.calendar_day_label, date.format(formatter), count)
                                    Surface(Modifier.weight(1f).clip(MaterialTheme.shapes.medium).selectable(active, role = Role.RadioButton) { selectedIso = date.toString() }.testTag("calendar-$date").semantics { contentDescription = description },
                                        shape = MaterialTheme.shapes.medium, color = if (active) MaterialTheme.colorScheme.primary else if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                        Column(Modifier.heightIn(min = 56.dp).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            Text(if (count > 0) "+$count" else "·", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.calendar_day_label, selected.format(formatter), counts[selected] ?: 0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            val events = tools.activity.filter { it.date == selected.toString() }
            if (events.isEmpty()) item { Text(stringResource(R.string.calendar_day_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(events) { event ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                        Text(event.title.ifBlank { "#${event.animeId}" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(if (event.episodeDelta > 0) "+${event.episodeDelta}" else event.episodeDelta.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMetric(label: String, value: String, modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NumberSetting(label: String, value: Int, minimum: Int, maximum: Int, step: Int, onChange: (Int) -> Unit, tag: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FilledTonalIconButton({ onChange((value - step).coerceAtLeast(minimum)) }, enabled = value > minimum, modifier = Modifier.testTag("$tag-decrease")) { Icon(Icons.Default.Remove, stringResource(R.string.value_decrease, label)) }
                Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                FilledTonalIconButton({ onChange((value + step).coerceAtMost(maximum)) }, enabled = value < maximum, modifier = Modifier.testTag("$tag-increase")) { Icon(Icons.Default.Add, stringResource(R.string.value_increase, label)) }
            }
        }
    }
}
