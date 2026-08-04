package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween
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
    val maxListHeight = LocalConfiguration.current.screenHeightDp.dp * 0.56f
    val motion = LocalMotionPolicy.current

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.notif_screen_title),
        trailingContent = {
            if (unread.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::markAllRead,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text(
                        "Mark all",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = motion.iosSpring()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NotificationTabs(
                selectedTab = selectedTab,
                unreadCount = unread.size,
                readCount = read.size,
                onTabSelected = { selectedTab = it },
            )

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp, max = maxListHeight),
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    (fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                        slideInHorizontally(
                            animationSpec = motion.iosTween(IosMotion.Standard),
                            initialOffsetX = { if (motion.animationsEnabled) direction * it / 10 else 0 },
                        )) togetherWith
                        (fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                            slideOutHorizontally(
                                animationSpec = motion.iosTween(IosMotion.Quick),
                                targetOffsetX = { if (motion.animationsEnabled) -direction * it / 14 else 0 },
                            ))
                },
                label = "notification-tab-content",
            ) { tab ->
                val list = if (tab == 0) unread else read
                if (list.isEmpty()) {
                    NotificationEmptyState(tab)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groupedByDay(list).forEach { (dayLabel, items) ->
                            item(key = "notification_day_$dayLabel") {
                                Text(
                                    text = dayLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
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
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        material = AppMaterial.Interactive,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            tabs.forEachIndexed { index, (labelRes, icon, count) ->
                val isSelected = selectedTab == index
                val fill by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "notification-tab-fill",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    label = "notification-tab-color",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clickable(role = Role.Tab) { onTabSelected(index) }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = fill,
                    contentColor = contentColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    NotificationTabContent(labelRes, icon, count, isSelected, contentColor)
                }
            }
        }
    }
}

@Composable
private fun NotificationTabContent(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    selected: Boolean,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(start = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
        )
        if (count > 0) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable
private fun NotificationEmptyState(selectedTab: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (selectedTab == 0) stringResource(R.string.notif_empty_unread)
            else stringResource(R.string.notif_empty_read),
            modifier = Modifier.padding(top = 9.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (selectedTab == 0) {
            Text(
                text = stringResource(R.string.notif_screen_empty_subtitle),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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
