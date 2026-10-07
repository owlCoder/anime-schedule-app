package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import com.owlcoder.animeschedule.domain.model.filteredActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchTools
import com.owlcoder.animeschedule.presentation.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** UTF-8 CSV with quoted cells and spreadsheet formula prefixes neutralized. */
internal fun List<MalListEntry>.toListCsv(tools: WatchTools): String = buildString {
    append("\uFEFFmal_id,title,status,episodes_watched,total_episodes,score,favorite,note,tags\r\n")
    sortedBy { it.animeId }.forEach { entry ->
        val cells = listOf(
            entry.animeId.toString(),
            entry.title,
            entry.status.malValue,
            entry.episodesWatched.toString(),
            entry.totalEpisodes?.toString().orEmpty(),
            entry.score.toString(),
            (entry.animeId in tools.favorites).toString(),
            tools.notes[entry.animeId].orEmpty(),
            tools.tags[entry.animeId].orEmpty().joinToString("; ")
        )
        append(cells.joinToString(",") { value ->
            val safe = if (value.trimStart().firstOrNull() in listOf(
                    '=',
                    '+',
                    '-',
                    '@'
                ) || value.startsWith('\t')
            ) "'$value" else value
            "\"${safe.replace("\"", "\"\"")}\""
        })
        append("\r\n")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListToolsSheet(
    canPick: Boolean,
    canExport: Boolean,
    onHistory: () -> Unit,
    onPick: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    continueTitle: String? = null,
    onContinue: () -> Unit = {},
) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.list_tools)) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolsAction(Icons.Default.PlayArrow, R.string.continue_watching, R.string.continue_watching_hint, continueTitle != null, onContinue, continueTitle)
        ToolsAction(
            Icons.Default.History,
            R.string.watch_history,
            R.string.watch_history_hint,
            true,
            onHistory
        )
        ToolsAction(
            Icons.Default.Shuffle,
            R.string.list_pick,
            R.string.list_pick_hint,
            canPick,
            onPick
        )
        ToolsAction(
            Icons.Default.FileDownload,
            R.string.list_export,
            R.string.list_export_hint,
            canExport,
            onExport
        )
        }
    }
}

@Composable
private fun ToolsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, subtitle: Int, enabled: Boolean, onClick: () -> Unit, subtitleText: String? = null) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppButton(stringResource(title), onClick, Modifier.fillMaxWidth(), variant = AppButtonVariant.Secondary, enabled = enabled, icon = icon)
        Text(subtitleText ?: stringResource(subtitle), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WatchHistorySheet(
    tools: WatchTools,
    onGoalChange: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalWatchTools.current.today
    val count = tools.episodesThisWeek(today)
    var clearConfirm by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var thisWeekOnly by remember { mutableStateOf(false) }
    val activities = tools.filteredActivity(query, thisWeekOnly, today)
    val locale = LocalConfiguration.current.locales[0]
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.watch_history)) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppMaterialSurface(shape = MaterialTheme.shapes.large) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            stringResource(R.string.weekly_goal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tools.weeklyGoal == 0) androidx.compose.ui.res.pluralStringResource(
                                R.plurals.weekly_goal_off_count,
                                count,
                                count
                            ) else androidx.compose.ui.res.pluralStringResource(
                                R.plurals.weekly_goal_progress_count,
                                count,
                                count,
                                tools.weeklyGoal
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (tools.weeklyGoal > 0) LinearProgressIndicator(progress = {
                            (count.toFloat() / tools.weeklyGoal).coerceIn(
                                0f,
                                1f
                            )
                        }, modifier = Modifier.fillMaxWidth().height(8.dp), drawStopIndicator = {})
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalIconButton(
                                { onGoalChange(tools.weeklyGoal - 1) },
                                enabled = tools.weeklyGoal > 0,
                                modifier = Modifier.testTag("goal-decrease")
                            ) { Icon(Icons.Default.Remove, stringResource(R.string.goal_decrease)) }
                            Text(
                                stringResource(R.string.weekly_goal_value, tools.weeklyGoal),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilledTonalIconButton(
                                { onGoalChange(tools.weeklyGoal + 1) },
                                enabled = tools.weeklyGoal < 100,
                                modifier = Modifier.testTag("goal-increase")
                            ) { Icon(Icons.Default.Add, stringResource(R.string.goal_increase)) }
                        }
                        Text(
                            stringResource(R.string.weekly_goal_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.watch_history_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                AppSearchField(query, { query = it }, Modifier.testTag("history-search"), stringResource(R.string.history_search), Icons.Default.Search, onClear = { query = "" })
                FilterChip(thisWeekOnly, { thisWeekOnly = !thisWeekOnly }, modifier = Modifier.testTag("history-this-week"),
                    label = { Text(stringResource(R.string.history_this_week)) }, leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) })
            }
            if (activities.isEmpty()) item {
                Text(
                    stringResource(R.string.history_empty),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
            items(activities) { activity ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (activity.episodeDelta > 0) Icons.Default.CheckCircle else Icons.Default.Edit,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            activity.title.ifBlank { "#${activity.animeId}" },
                            style = MaterialTheme.typography.titleSmall
                        )
                        val date = runCatching {
                            LocalDate.parse(activity.date)
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                        }.getOrDefault(activity.date)
                        Text(
                            "$date · ${
                                stringResource(
                                    R.string.history_episode,
                                    activity.progress
                                )
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        if (activity.episodeDelta > 0) "+${activity.episodeDelta}" else activity.episodeDelta.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (tools.activity.isNotEmpty()) item {
                if (clearConfirm) {
                    Text(stringResource(R.string.history_clear_confirm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        AppButton(stringResource(R.string.common_cancel), { clearConfirm = false }, modifier = Modifier.weight(1f), variant = AppButtonVariant.Plain, icon = Icons.Default.Close)
                        AppButton(stringResource(R.string.history_clear), { onClear(); clearConfirm = false }, modifier = Modifier.weight(1f), variant = AppButtonVariant.Destructive, icon = Icons.Default.DeleteOutline)
                    }
                } else AppButton(stringResource(R.string.history_clear), { clearConfirm = true }, variant = AppButtonVariant.Plain, icon = Icons.Default.DeleteOutline)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnimePickSheet(
    entry: MalListEntry?,
    onOpen: (MalListEntry) -> Unit,
    onReroll: () -> Unit,
    onDismiss: () -> Unit
) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.list_pick)) {
        if (entry != null) {
            Row(
                Modifier.padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MediaThumbnail.Small(
                    url = entry.coverImageUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.size(78.dp, 110.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(entry.status.displayName(), color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.history_episode, entry.episodesWatched),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton(stringResource(R.string.pick_again), onReroll, Modifier.weight(1f), variant = AppButtonVariant.Secondary, icon = Icons.Default.Shuffle)
                AppButton(stringResource(R.string.pick_open), { onOpen(entry) }, Modifier.weight(1f), icon = Icons.Default.PlayArrow)
            }
        } else Text(stringResource(R.string.pick_empty), modifier = Modifier.padding(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagFilterSheet(tags: List<String>, selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.personal_tags)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AppChoiceRow(stringResource(R.string.tags_all), Icons.Default.LabelOff, selected == null, { onSelect(null) }) }
            if (tags.isEmpty()) item { Text(stringResource(R.string.tags_empty), style = MaterialTheme.typography.bodyMedium) }
            items(tags, key = { it }) { tag -> AppChoiceRow(tag, Icons.Default.Label, selected.equals(tag, true), { onSelect(tag) }) }
        }
    }
}
