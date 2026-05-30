package rs.owlcoder.animeschedule.presentation.screens.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import rs.owlcoder.animeschedule.R
import rs.owlcoder.animeschedule.domain.model.AppNotification
import rs.owlcoder.animeschedule.presentation.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private data class NotifTab(
    val labelRes: Int,
    val icon: ImageVector,
    val badge: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onAnimeClick: (Int) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unread = notifications.filter { !it.isRead }
    val read = notifications.filter { it.isRead }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NotifTab(R.string.notif_tab_unread, Icons.Default.MarkEmailUnread, unread.size),
        NotifTab(R.string.notif_tab_read, Icons.Default.MarkEmailRead)
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.notif_screen_title), style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        if (unread.isNotEmpty()) {
                            IconButton(onClick = { viewModel.markAllRead() }) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
                // iOS-style segmented pill — isti stil kao Schedule ekran
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = index == selectedTab
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.surface
                                              else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "notif_tab_bg"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                              else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "notif_tab_fg"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgColor)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectedTab = index }
                                    )
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = contentColor
                                        )
                                        if (tab.badge > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                    val label = if (tab.badge > 0)
                                        "${stringResource(tab.labelRes)} (${tab.badge})"
                                    else
                                        stringResource(tab.labelRes)
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    ) { innerPadding ->
        val list = if (selectedTab == 0) unread else read
        val emptyTitle = if (selectedTab == 0)
            stringResource(R.string.notif_empty_unread)
        else
            stringResource(R.string.notif_empty_read)

        if (list.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = emptyTitle,
                subtitle = if (selectedTab == 0) stringResource(R.string.notif_screen_empty_subtitle) else "",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
            ) {
                items(list, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            viewModel.markRead(notification.id)
                            onAnimeClick(notification.animeId)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val cardBg = if (!notification.isRead)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = notification.coverImageUrl,
            contentDescription = notification.title,
            modifier = Modifier
                .size(width = 48.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Epizoda ${notification.episode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = relativeTime(notification.createdAtEpochSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun relativeTime(epochSeconds: Long): String {
    val now = Instant.now()
    val time = Instant.ofEpochSecond(epochSeconds)
    val minutesAgo = ChronoUnit.MINUTES.between(time, now)
    return when {
        minutesAgo < 1 -> "Upravo"
        minutesAgo < 60 -> "Pre ${minutesAgo}min"
        minutesAgo < 1440 -> "Pre ${minutesAgo / 60}h"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d.M.yyyy")
            time.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }
}
