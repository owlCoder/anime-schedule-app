package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.DiscoveryChip
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryInsightsSheet(state: MyListUiState, onDismiss: () -> Unit) {
    var period by rememberSaveable { mutableIntStateOf(30) }
    val today = LocalWatchTools.current.today
    val summary = remember(state.tools.activity, today, period) { state.tools.activitySummary(today, period) }
    val forecast = remember(state.allEntries, state.tools.activity, today) { state.allEntries.finishForecast(state.tools, today) }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.mylist_statistics)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 620.dp).testTag("library-insights"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.library_total_titles), state.insights.totalAnime.toString(), Modifier.weight(1f).fillMaxHeight())
                    Metric(stringResource(R.string.mylist_metric_episodes), state.insights.watchedEpisodes.toString(), Modifier.weight(1f).fillMaxHeight())
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.mylist_metric_completed), state.insights.completedAnime.toString(), Modifier.weight(1f).fillMaxHeight())
                    Metric(stringResource(R.string.mylist_metric_score), state.insights.averageScore?.let { String.format(locale, "%.1f", it) } ?: "—", Modifier.weight(1f).fillMaxHeight())
                }
            }
            item { Text(stringResource(R.string.mylist_backlog, state.insights.remainingEpisodes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item { Text(stringResource(R.string.mylist_rated_count, state.insights.ratedAnime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(listOf(WatchStatus.WATCHING, WatchStatus.COMPLETED, WatchStatus.ON_HOLD, WatchStatus.DROPPED, WatchStatus.PLAN_TO_WATCH)) { status ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(status.displayName(), style = MaterialTheme.typography.bodyMedium)
                    Text((state.statusCounts[status] ?: 0).toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            item { HorizontalDivider(); Text(stringResource(R.string.activity_insights), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { Text(stringResource(R.string.activity_local_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30, 90).forEach { days -> DiscoveryChip(androidx.compose.ui.res.pluralStringResource(R.plurals.activity_period, days, days), Icons.Default.DateRange, period == days, { period = days }, Modifier.weight(1f).fillMaxHeight().testTag("insights-period-$days")) }
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.activity_episode_total), summary.episodes.toString(), Modifier.weight(1f).fillMaxHeight())
                    Metric(stringResource(R.string.activity_active_days), summary.activeDays.toString(), Modifier.weight(1f).fillMaxHeight())
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.activity_daily_average), String.format(locale, "%.1f", summary.dailyAverage), Modifier.weight(1f).fillMaxHeight().testTag("insights-average"))
                    Metric(stringResource(R.string.activity_previous_change), (if (summary.change > 0) "+" else "") + summary.change, Modifier.weight(1f).fillMaxHeight().testTag("insights-change"))
                }
            }
            item { Text(androidx.compose.ui.res.pluralStringResource(R.plurals.activity_previous_total, summary.previousEpisodes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), summary.previousEpisodes, period), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                Text(stringResource(R.string.activity_trend), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val max = summary.trend.maxOf { it.second }.coerceAtLeast(1)
                val weekday = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.trend.forEach { (day, count) ->
                        Column(Modifier.testTag("activity-trend-$day"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(day.format(weekday), style = MaterialTheme.typography.labelMedium); Text(count.toString(), style = MaterialTheme.typography.labelMedium) }
                            LinearProgressIndicator(progress = { count.toFloat() / max }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, gapSize = 0.dp, drawStopIndicator = {})
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.activity_top_titles), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            if (summary.titles.isEmpty()) item { Text(stringResource(R.string.activity_no_progress), style = MaterialTheme.typography.bodySmall) }
            items(summary.titles.take(5), key = { "top-${it.animeId}" }) { title ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Text(title.title.ifBlank { "#${title.animeId}" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(title.episodes.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Metric(stringResource(R.string.activity_finish_forecast), forecast?.format(dateFormat) ?: stringResource(R.string.activity_forecast_unknown), Modifier.fillMaxWidth())
                Text(stringResource(R.string.activity_forecast_hint), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
