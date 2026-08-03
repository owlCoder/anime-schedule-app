package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A short, transient notification sheet with grouped inset rows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()
    val unread = notifications.filter { !it.isRead }
    val read = notifications.filter { it.isRead }
    var selectedTab by remember { mutableIntStateOf(0) }
    val list = if (selectedTab == 0) unread else read
    val maxSheetHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.72f

    AppSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = maxSheetHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppInlineHeader(
                title = stringResource(R.string.notif_screen_title),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        if (unread.isNotEmpty()) {
                            Text(
                                text = "${unread.size}",
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (unread.isNotEmpty()) {
                            TextButton(onClick = viewModel::markAllRead) {
                                Icon(Icons.Default.DoneAll, contentDescription = null)
                                Text("Mark all")
                            }
                        }
                    }
                },
            )

            NotificationTabs(
                selectedTab = selectedTab,
                unreadCount = unread.size,
                readCount = read.size,
                onTabSelected = { selectedTab = it },
            )

            if (list.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Notifications,
                    title = if (selectedTab == 0) stringResource(R.string.notif_empty_unread)
                    else stringResource(R.string.notif_empty_read),
                    subtitle = if (selectedTab == 0) stringResource(R.string.notif_screen_empty_subtitle) else null,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    groupedByDay(list).forEach { (dayLabel, items) ->
                        item(key = "notification_day_$dayLabel") {
                            Text(
                                text = dayLabel,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        item(key = "notification_group_$dayLabel") {
                            InsetGroup {
                                items.forEach { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onClick = {
                                            viewModel.markRead(notification.id)
                                            onDismiss()
                                            onAnimeClick(notification.animeId)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTabs(
    selectedTab: Int,
    unreadCount: Int,
    readCount: Int,
    onTabSelected: (Int) -> Unit,
) {
    val tabs = listOf(
        Triple(R.string.notif_tab_unread, Icons.Default.MarkEmailUnread, unreadCount),
        Triple(R.string.notif_tab_read, Icons.Default.MarkEmailRead, readCount),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tabs.forEachIndexed { index, (labelRes, icon, count) ->
            val isSelected = selectedTab == index
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else Color.Transparent,
                    )
                    .clickable(
                        role = Role.Tab,
                        onClick = { onTabSelected(index) },
                    )
                    .semantics { role = Role.Tab },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (count > 0) {
                    Text(
                        text = "  $count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun groupedByDay(
    notifications: List<AppNotification>,
): List<Pair<String, List<AppNotification>>> {
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault())
    return notifications
        .sortedByDescending { it.createdAtEpochSeconds }
        .groupBy { Instant.ofEpochSecond(it.createdAtEpochSeconds).atZone(zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, items) ->
            date.format(formatter) to items
        }
}
