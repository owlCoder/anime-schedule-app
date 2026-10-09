package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.time.currentDateFlow
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.DiscoveryChip
import com.owlcoder.animeschedule.presentation.screens.schedule.LocalScheduleZone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsOverlay(onAnimeClick: (Int) -> Unit, onDismiss: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val (unread, read) = remember(notifications) { notifications.partition { !it.isRead } }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var period by rememberSaveable { mutableStateOf(NotificationPeriod.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(NotificationSort.NEWEST) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var showTools by rememberSaveable { mutableStateOf(false) }
    val clearing by viewModel.clearing.collectAsStateWithLifecycle()
    val clearError by viewModel.clearError.collectAsStateWithLifecycle()
    val marking by viewModel.marking.collectAsStateWithLifecycle()
    val markError by viewModel.markError.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val zone = LocalScheduleZone.current
    val today by remember(zone) { currentDateFlow(zone) }.collectAsStateWithLifecycle(initialValue = LocalDate.now(zone))
    val visibleUnread = remember(unread, query, period, today, zone, sort) { unread.visibleNotifications(query, period, today, zone, sort) }
    val visible = if (selectedTab == 0) visibleUnread else remember(read, query, period, today, zone, sort) {
        read.visibleNotifications(query, period, today, zone, sort)
    }
    val historyState = rememberLazyListState()
    val toolsState = rememberLazyListState()
    LaunchedEffect(sort) { historyState.scrollToItem(0) }
    // The already sorted rows determine group order. Date keys include the year, independent of locale.
    val grouped = remember(visible, zone) { visible.groupBy { Instant.ofEpochSecond(it.createdAtEpochSeconds).atZone(zone).toLocalDate() } }
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", locale) }
    AppSheet(onDismissRequest = onDismiss, onNavigateBack = {
        if (showTools) { showTools = false; true } else false
    },
        title = stringResource(if (showTools) R.string.notif_tools else R.string.notif_screen_title),
        trailingContent = { if (!showTools) GlassIconButton(Icons.Default.Tune, stringResource(R.string.notif_tools), { showTools = true }, Modifier.testTag("notif-tools")) }) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        fun finishInput() { focus.clearFocus(force = true); keyboard?.hide() }
        LaunchedEffect(showTools) { finishInput() }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 650.dp).testTag(if (showTools) "notif-tools-list" else "notif-history"),
            state = if (showTools) toolsState else historyState,
            verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            if (showTools) {
                item { Text(stringResource(R.string.notif_sort), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(NotificationSort.entries, key = { "sort-$it" }) { option ->
                    AppChoiceRow(stringResource(if (option == NotificationSort.NEWEST) R.string.notif_newest else R.string.notif_oldest),
                        Icons.Default.Sort, option == sort, { sort = option }, Modifier.testTag("notif-sort-$option"))
                }
                item { Text(stringResource(R.string.notif_scope_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { AppButton(stringResource(R.string.notif_mark_visible, visibleUnread.size), { viewModel.markVisibleRead(visibleUnread.map { it.id }) },
                    Modifier.fillMaxWidth().testTag("notif-mark-visible"), enabled = visibleUnread.isNotEmpty() && !marking, icon = Icons.Default.MarkEmailRead) }
                item { AppButton(stringResource(R.string.notifications_mark_all), viewModel::markAllRead,
                    Modifier.fillMaxWidth().testTag("notif-mark-all"), enabled = unread.isNotEmpty() && !marking, variant = AppButtonVariant.Secondary, icon = Icons.Default.DoneAll) }
                if (markError) item { Text(stringResource(R.string.notif_mark_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            } else {
                item("search") { AppSearchField(query, { query = it }, Modifier.testTag("notification-search"), stringResource(R.string.notif_search), Icons.Default.Search, onClear = { query = "" }) }
                item("tabs") { AppSegmentedControl(listOf(
                    SegmentOption(stringResource(R.string.notif_tab_unread), Icons.Default.MarkEmailUnread, unread.size),
                    SegmentOption(stringResource(R.string.notif_tab_read), Icons.Default.MarkEmailRead, read.size)), selectedTab,
                    { selectedTab = it; confirmClear = false; finishInput() }) }
                item("periods") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationPeriod.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { option -> DiscoveryChip(stringResource(when (option) {
                                NotificationPeriod.ALL -> R.string.notif_period_all
                                NotificationPeriod.TODAY -> R.string.notif_period_today
                                NotificationPeriod.WEEK -> R.string.notif_period_week
                                NotificationPeriod.MONTH -> R.string.notif_period_month
                            }), Icons.Default.DateRange, option == period, { period = option; finishInput() }, Modifier.weight(1f).fillMaxHeight().testTag("notif-period-$option")) }
                        }
                    }
                } }
                if (selectedTab == 1 && read.isNotEmpty()) item("clear") {
                    if (confirmClear) Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.notif_clear_confirm, read.size), style = MaterialTheme.typography.bodyMedium)
                            AppButton(stringResource(R.string.notif_clear_read), { viewModel.clearRead(); confirmClear = false }, Modifier.fillMaxWidth().testTag("notif-clear-confirm"), enabled = !clearing, variant = AppButtonVariant.Destructive, icon = Icons.Default.DeleteOutline)
                            AppButton(stringResource(R.string.common_cancel), { confirmClear = false }, Modifier.fillMaxWidth().testTag("notif-clear-cancel"), enabled = !clearing, variant = AppButtonVariant.Plain, icon = Icons.Default.Close)
                        }
                    } else AppButton(stringResource(R.string.notif_clear_read), { finishInput(); confirmClear = true }, Modifier.fillMaxWidth().testTag("notif-clear-read"), enabled = !clearing, variant = AppButtonVariant.Plain, icon = Icons.Default.DeleteOutline)
                }
                if (clearError) item("error") { Text(stringResource(R.string.notif_clear_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (markError) item("mark-error") { Text(stringResource(R.string.notif_mark_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (visible.isEmpty()) item("empty") {
                    val emptyModifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                    when {
                        query.isNotBlank() -> EmptyState(Icons.Default.SearchOff, stringResource(R.string.notif_search_empty), actionLabel = stringResource(R.string.search_clear_query), onAction = { query = ""; finishInput() }, modifier = emptyModifier)
                        period != NotificationPeriod.ALL -> EmptyState(Icons.Default.DateRange, stringResource(R.string.notif_period_empty), actionLabel = stringResource(R.string.notif_period_reset), onAction = { period = NotificationPeriod.ALL }, modifier = emptyModifier)
                        else -> EmptyState(Icons.Outlined.NotificationsNone, stringResource(if (selectedTab == 0) R.string.notif_empty_unread else R.string.notif_empty_read), subtitle = if (selectedTab == 0) stringResource(R.string.notif_screen_empty_subtitle) else null, modifier = emptyModifier)
                    }
                }
                grouped.forEach { (date, rows) ->
                    item("day-${date.toEpochDay()}") { Text(date.format(formatter), Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
                    items(rows, key = { "notification-${it.id}" }) { notification -> NotificationCard(notification, onClick = {
                        viewModel.markRead(notification.id); onDismiss(); onAnimeClick(notification.animeId)
                    }, onReadChange = { read -> viewModel.setRead(notification.id, read) }, readActionEnabled = !marking) }
                }
            }
        }
    }
}
