package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val maxListHeight = LocalConfiguration.current.screenHeightDp.dp * 0.52f

    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = stringResource(R.string.notif_screen_title),
        trailingContent = {
            if (unread.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::markAllRead,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Mark all", modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp, max = 230.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = maxListHeight),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    groupedByDay(list).forEach { (dayLabel, items) ->
                        item(key = "notification_day_$dayLabel") {
                            Text(
                                text = dayLabel,
                                modifier = Modifier.padding(horizontal = 10.dp),
                                style = MaterialTheme.typography.labelSmall,
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
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        tabs.forEachIndexed { index, (labelRes, icon, count) ->
            val isSelected = selectedTab == index
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else Color.Transparent,
                    )
                    .clickable(role = Role.Tab) { onTabSelected(index) }
                    .semantics { role = Role.Tab },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(labelRes),
                    modifier = Modifier.padding(start = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 5.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
        .map { (date, items) -> date.format(formatter) to items }
}
