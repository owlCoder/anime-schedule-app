package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.ScheduleDay
import com.owlcoder.animeschedule.presentation.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleAgendaSheet(days: List<ScheduleDay>, selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit, onExport: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val episodes = remember(days) { days.flatMap { it.episodes } }
    val busiest = remember(days) { days.maxByOrNull { it.episodes.size }?.takeIf { it.episodes.isNotEmpty() } }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.schedule_agenda)) {
        Column(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazyColumn(Modifier.weight(1f, fill = false).testTag("agenda-days"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.schedule_agenda_total, episodes.size, episodes.map { it.animeId }.distinct().size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("agenda-total"))
                            busiest?.let { Text(stringResource(R.string.schedule_agenda_busiest, it.date.format(dateFormat), broadcastLabel(it.episodes.size)), style = MaterialTheme.typography.bodySmall) }
                            Text(stringResource(R.string.schedule_agenda_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                if (episodes.isEmpty()) item { Text(stringResource(R.string.schedule_agenda_empty), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(4.dp)) }
                items(days, key = { it.date.toEpochDay() }) { day ->
                    AppChoiceRow(day.date.format(dateFormat), Icons.Default.CalendarToday, day.date == selectedDate,
                        { onDateSelected(day.date) }, Modifier.testTag("agenda-day-${day.date}"),
                        subtitle = broadcastLabel(day.episodes.size))
                }
            }
            AppButton(stringResource(R.string.schedule_agenda_share), onShare, Modifier.fillMaxWidth().testTag("agenda-share"),
                enabled = days.firstOrNull { it.date == selectedDate }?.episodes?.isNotEmpty() == true, variant = AppButtonVariant.Secondary, icon = Icons.Default.Share)
            AppButton(stringResource(R.string.schedule_agenda_export), onExport, Modifier.fillMaxWidth().testTag("agenda-export"), enabled = episodes.isNotEmpty(), icon = Icons.Default.EventAvailable)
        }
    }
}

@Composable
private fun broadcastLabel(count: Int): String = when (count) {
    0 -> stringResource(R.string.schedule_no_broadcasts)
    1 -> stringResource(R.string.schedule_one_broadcast)
    else -> stringResource(R.string.schedule_broadcasts_count, count)
}
