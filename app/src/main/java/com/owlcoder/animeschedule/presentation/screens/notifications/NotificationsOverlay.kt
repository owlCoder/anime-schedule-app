package com.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppSearchField
import com.owlcoder.animeschedule.presentation.components.EmptyState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.screens.schedule.LocalScheduleZone
import com.owlcoder.animeschedule.domain.model.AppNotification
import com.owlcoder.animeschedule.presentation.components.AppSegmentedControl
import com.owlcoder.animeschedule.presentation.components.SegmentOption
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val (unread, read) = remember(notifications) {
        notifications.partition { notification -> !notification.isRead }
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val clearing by viewModel.clearing.collectAsStateWithLifecycle()
    val clearError by viewModel.clearError.collectAsStateWithLifecycle()
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val density = LocalDensity.current
    val maxListHeight = remember(windowHeightPx, density) { with(density) { (windowHeightPx * 0.38f).toDp() } }
    val motion = LocalMotionPolicy.current
    val appLocale = LocalConfiguration.current.locales[0]
    val zoneId = LocalScheduleZone.current

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.notif_screen_title),
        trailingContent = {
            if (unread.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::markAllRead,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = stringResource(R.string.notifications_mark_all),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) {
        val focus = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = motion.iosSpring()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppSearchField(query, { query = it }, Modifier.testTag("notification-search"), stringResource(R.string.notif_search), Icons.Default.Search, onClear = { query = "" })
            NotificationTabs(
                selectedTab = selectedTab,
                unreadCount = unread.size,
                readCount = read.size,
                onTabSelected = { selectedTab = it; confirmClear = false; focus.clearFocus(); keyboard?.hide() },
            )

            if (selectedTab == 1 && read.isNotEmpty()) {
                if (confirmClear) {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.notif_clear_confirm, read.size), style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton(stringResource(R.string.common_cancel), { confirmClear = false }, Modifier.weight(1f).testTag("notif-clear-cancel"), enabled = !clearing, variant = AppButtonVariant.Plain, icon = Icons.Default.Close)
                                AppButton(stringResource(R.string.notif_clear_read), { viewModel.clearRead(); confirmClear = false }, Modifier.weight(1f).testTag("notif-clear-confirm"), enabled = !clearing, variant = AppButtonVariant.Destructive, icon = Icons.Default.DeleteOutline)
                            }
                        }
                    }
                } else AppButton(stringResource(R.string.notif_clear_read), { focus.clearFocus(); keyboard?.hide(); confirmClear = true }, Modifier.fillMaxWidth().testTag("notif-clear-read"), enabled = !clearing, variant = AppButtonVariant.Plain, icon = Icons.Default.DeleteOutline)
            }
            if (clearError) Text(stringResource(R.string.notif_clear_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxListHeight.coerceAtLeast(180.dp)),
                transitionSpec = {
                    (fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                        scaleIn(
                            initialScale = 0.985f,
                            animationSpec = motion.iosTween(IosMotion.Standard),
                        )) togetherWith
                        (fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                            scaleOut(
                                targetScale = 0.995f,
                                animationSpec = motion.iosTween(IosMotion.Quick),
                            ))
                },
                label = "notification-tab-content",
            ) { tab ->
                val source = if (tab == 0) unread else read
                val list = remember(source, query) { source.filter { it.title.contains(query.trim(), ignoreCase = true) } }
                if (list.isEmpty()) {
                    if (query.isNotBlank()) EmptyState(Icons.Default.SearchOff, stringResource(R.string.notif_search_empty), actionLabel = stringResource(R.string.search_clear_query), onAction = { query = ""; focus.clearFocus(); keyboard?.hide() }, modifier = Modifier.fillMaxSize()) else NotificationEmptyState(tab)
                } else {
                    val groupedNotifications = remember(list, appLocale, zoneId) { groupedByDay(list, appLocale, zoneId) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        groupedNotifications.forEach { (dayLabel, items) ->
                            item(key = "notification_day_$dayLabel") {
                                Text(
                                    text = dayLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            items(items, key = { it.id }) { notification ->
                                NotificationCard(notification, onClick = {
                                    viewModel.markRead(notification.id)
                                    onDismiss()
                                    onAnimeClick(notification.animeId)
                                })
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
    AppSegmentedControl(
        options = listOf(
            SegmentOption(stringResource(R.string.notif_tab_unread), Icons.Default.MarkEmailUnread, unreadCount),
            SegmentOption(stringResource(R.string.notif_tab_read), Icons.Default.MarkEmailRead, readCount),
        ), selectedIndex = selectedTab, onSelect = onTabSelected,
    )
}

@Composable
private fun NotificationEmptyState(selectedTab: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(82.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.075f),
            contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        Text(
            text = if (selectedTab == 0) {
                stringResource(R.string.notif_empty_unread)
            } else {
                stringResource(R.string.notif_empty_read)
            },
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (selectedTab == 0) {
            Text(
                text = stringResource(R.string.notif_screen_empty_subtitle),
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun groupedByDay(
    notifications: List<AppNotification>,
    locale: Locale,
    zone: ZoneId,
): List<Pair<String, List<AppNotification>>> {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", locale)
    return notifications
        .sortedByDescending { it.createdAtEpochSeconds }
        .groupBy { notification ->
            Instant.ofEpochSecond(notification.createdAtEpochSeconds).atZone(zone).toLocalDate()
        }
        .toSortedMap(compareByDescending { it })
        .map { (date, items) -> date.format(formatter) to items }
}
