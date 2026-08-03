package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppSurface
import com.owlcoder.animeschedule.presentation.components.CountdownText
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LoadingShimmer
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.screens.notifications.NotificationsOverlay
import com.owlcoder.animeschedule.presentation.screens.seasonal.SeasonalOverlay
import com.owlcoder.animeschedule.ui.theme.GlassTone
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (Int) -> Unit,
    onInitialLoadChange: (Boolean) -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val openOverlay by viewModel.openOverlay.collectAsState()
    val toast = LocalToast.current
    var editingEpisode by remember { mutableStateOf<AiringEpisode?>(null) }
    var lastIncrementedEpisode by remember { mutableStateOf<AiringEpisode?>(null) }

    val markedMsg = stringResource(R.string.toast_episode_marked)
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)
    val scheduleErrorMsg = uiState.errorRes?.let { stringResource(it) }

    LaunchedEffect(uiState.isInitialLoad) { onInitialLoadChange(uiState.isInitialLoad) }
    LaunchedEffect(scheduleErrorMsg) { scheduleErrorMsg?.let(toast::error) }
    LaunchedEffect(Unit) {
        viewModel.incrementEvent.collect { event ->
            when (event) {
                ScheduleViewModel.IncrementEvent.Success -> {
                    toast.success(markedMsg)
                    val episode = lastIncrementedEpisode
                    val entry = episode?.malListEntry
                    val total = entry?.totalEpisodes ?: episode?.totalEpisodes
                    if (episode != null && entry != null && total != null && entry.episodesWatched + 1 >= total) {
                        editingEpisode = episode.copy(
                            malListEntry = entry.copy(
                                episodesWatched = total,
                                status = com.owlcoder.animeschedule.domain.model.WatchStatus.COMPLETED,
                            ),
                        )
                    }
                }
                ScheduleViewModel.IncrementEvent.Updated -> toast.success(savedMsg)
                ScheduleViewModel.IncrementEvent.Removed -> toast.success(removedMsg)
                ScheduleViewModel.IncrementEvent.Error -> toast.error(errorMsg)
            }
            lastIncrementedEpisode = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            when {
                uiState.isInitialLoad -> Unit
                uiState.isLoading && uiState.todayEpisodes.isEmpty() -> LoadingShimmer()
                else -> TodayHomeContent(
                    uiState = uiState,
                    scheduleError = scheduleErrorMsg,
                    onRetry = viewModel::refresh,
                    onCardClick = { onAnimeClick(it.animeId) },
                    onIncrementEpisode = { episode ->
                        episode.malId?.let { malId ->
                            lastIncrementedEpisode = episode
                            viewModel.incrementEpisode(malId)
                        }
                    },
                    onEditStatus = { editingEpisode = it },
                    onSeeAll = { viewModel.setOpenOverlay(ScheduleOverlay.SeeAll(ScheduleSection.TODAY)) },
                    onSeasonal = { viewModel.setOpenOverlay(ScheduleOverlay.Seasonal) },
                    onNotifications = { viewModel.setOpenOverlay(ScheduleOverlay.Notifications) },
                    onFilter = { viewModel.setOpenOverlay(ScheduleOverlay.Filter) },
                )
            }
        }
    }

    editingEpisode?.malId?.let { malId ->
        ListStatusBottomSheet(
            animeId = malId,
            currentEntry = editingEpisode?.malListEntry,
            onDismiss = { editingEpisode = null },
            onConfirm = { id, update -> viewModel.updateEntry(id, update) },
            onRemove = { id -> viewModel.removeEntry(id) },
        )
    }

    if (openOverlay is ScheduleOverlay.Filter) {
        ScheduleFilterSheet(
            filter = uiState.filter,
            availableGenres = uiState.availableGenres,
            availableFormats = uiState.availableFormats,
            isLoggedIn = uiState.isLoggedIn,
            onOnlyMyListChange = viewModel::setOnlyMyList,
            onGenreToggle = viewModel::toggleGenre,
            onFormatToggle = viewModel::toggleFormat,
            onClear = viewModel::clearFilter,
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
        )
    }

    if (openOverlay is ScheduleOverlay.SeeAll) {
        SeeAllSheet(
            title = stringResource(R.string.schedule_section_today),
            episodes = uiState.todayEpisodes,
            onCardClick = { onAnimeClick(it.animeId) },
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
            pendingIncrementIds = uiState.pendingIncrementIds,
            onIncrementEpisode = { episode ->
                episode.malId?.let { malId ->
                    lastIncrementedEpisode = episode
                    viewModel.incrementEpisode(malId)
                }
            },
            isLoggedIn = uiState.isLoggedIn,
            onEditStatus = { editingEpisode = it },
        )
    }

    if (openOverlay is ScheduleOverlay.Notifications) {
        NotificationsOverlay(
            onAnimeClick = onAnimeClick,
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
        )
    }
    if (openOverlay is ScheduleOverlay.Seasonal) {
        SeasonalOverlay(
            onAnimeClick = onAnimeClick,
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
        )
    }
}

@Composable
private fun TodayHomeContent(
    uiState: ScheduleUiState,
    scheduleError: String?,
    onRetry: () -> Unit,
    onCardClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit,
    onEditStatus: (AiringEpisode) -> Unit,
    onSeeAll: () -> Unit,
    onSeasonal: () -> Unit,
    onNotifications: () -> Unit,
    onFilter: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val date = remember { dateFormatter.format(LocalDate.now()).replaceFirstChar { it.titlecase(Locale.getDefault()) } }
    val selection = DashboardScheduleSelector.select(uiState.todayEpisodes, Clock.systemDefaultZone())
    val hasSchedule = uiState.todayEpisodes.isNotEmpty()
    val notificationDescription = if (uiState.unreadNotificationCount > 0) {
        "${uiState.unreadNotificationCount} ${stringResource(R.string.schedule_notifications_action)}"
    } else {
        stringResource(R.string.schedule_notifications_action)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = LocalNavBarHeight.current + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "today-header") {
            AppLargeHeader(
                title = stringResource(R.string.schedule_tab_today),
                subtitle = date,
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        CompactToolbarAction(
                            icon = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.nav_seasonal),
                            onClick = onSeasonal,
                        )
                        CompactToolbarAction(
                            icon = Icons.Default.Notifications,
                            contentDescription = notificationDescription,
                            onClick = onNotifications,
                        )
                        CompactToolbarAction(
                            icon = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.filter_title),
                            onClick = onFilter,
                            accented = uiState.filter.isActive,
                        )
                    }
                },
            )
        }

        scheduleError?.let { error -> item(key = "schedule-error") { ErrorBanner(error, onRetry) } }

        if (!hasSchedule) {
            item(key = "schedule-empty") {
                EmptyState(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.schedule_empty_title),
                    subtitle = stringResource(R.string.schedule_empty_subtitle),
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
            }
        } else {
            selection.featured?.let { episode ->
                item(key = "featured-${episode.airingId}") {
                    FeaturedAiring(
                        episode = episode,
                        isLoggedIn = uiState.isLoggedIn,
                        isIncrementing = episode.malId in uiState.pendingIncrementIds,
                        onClick = { onCardClick(episode) },
                        onIncrement = { onIncrementEpisode(episode) },
                    )
                }
            }

            item(key = "up-next-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Next 90 minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (selection.upcoming.isEmpty()) "Nothing else soon" else "${selection.upcoming.size} upcoming",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onSeeAll, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.schedule_see_all))
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (selection.upcoming.isNotEmpty()) {
                item(key = "upcoming-list") {
                    UpcomingAiringList(
                        episodes = selection.upcoming,
                        isLoggedIn = uiState.isLoggedIn,
                        pendingIncrementIds = uiState.pendingIncrementIds,
                        onCardClick = onCardClick,
                        onIncrementEpisode = onIncrementEpisode,
                        onEditStatus = onEditStatus,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToolbarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    accented: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            tone = if (accented) GlassTone.Accent else GlassTone.Neutral,
            contentColor = if (accented) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FeaturedAiring(
    episode: AiringEpisode,
    isLoggedIn: Boolean,
    isIncrementing: Boolean,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
) {
    val groupedShape = MaterialTheme.shapes.medium
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp, max = 108.dp)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                shape = groupedShape,
            )
            .clickable(onClick = onClick),
        shape = groupedShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaThumbnail.Small(
                url = episode.coverImageUrl,
                contentDescription = episode.title,
                modifier = Modifier.size(width = 64.dp, height = 80.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (episode.airingAtEpochSeconds <= System.currentTimeMillis() / 1000) "Now airing" else "Up next",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(episode.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.schedule_episode_label, episode.episode, episode.totalEpisodes?.let { "/$it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CountdownText(episode.airingAtEpochSeconds)
                }
            }
            if (isLoggedIn && episode.malListEntry != null) {
                GlassButton(
                    onClick = onIncrement,
                    enabled = !isIncrementing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) { contentColor ->
                    if (isIncrementing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = contentColor)
                    else Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_hero_action_watched), modifier = Modifier.size(18.dp), tint = contentColor)
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UpcomingAiringList(
    episodes: List<AiringEpisode>,
    isLoggedIn: Boolean,
    pendingIncrementIds: Set<Int>,
    onCardClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit,
    onEditStatus: (AiringEpisode) -> Unit,
) {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Column {
            episodes.forEachIndexed { index, episode ->
                UpcomingAiringRow(
                    episode = episode,
                    isLoggedIn = isLoggedIn,
                    isIncrementing = episode.malId in pendingIncrementIds,
                    onClick = { onCardClick(episode) },
                    onIncrement = { onIncrementEpisode(episode) },
                    onEditStatus = { onEditStatus(episode) },
                )
                if (index < episodes.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 82.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun UpcomingAiringRow(
    episode: AiringEpisode,
    isLoggedIn: Boolean,
    isIncrementing: Boolean,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onEditStatus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp, max = 72.dp).clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(airingTimeLabel(episode), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            CountdownText(episode.airingAtEpochSeconds)
        }
        MediaThumbnail.Small(url = episode.coverImageUrl, contentDescription = episode.title, modifier = Modifier.size(width = 44.dp, height = 56.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(episode.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.schedule_episode_label, episode.episode, episode.totalEpisodes?.let { "/$it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isLoggedIn && episode.malListEntry != null) {
            androidx.compose.material3.IconButton(onClick = onIncrement, enabled = !isIncrementing, modifier = Modifier.size(48.dp)) {
                if (isIncrementing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Add, contentDescription = stringResource(R.string.schedule_hero_action_watched), tint = MaterialTheme.colorScheme.primary)
            }
            androidx.compose.material3.IconButton(onClick = onEditStatus, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.schedule_edit_status_action))
            }
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Full chronological list intended for the shell's future All Today route. */
@Composable
fun AllTodayScreen(
    episodes: List<AiringEpisode>,
    isLoggedIn: Boolean,
    pendingIncrementIds: Set<Int> = emptySet(),
    onAnimeClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit = {},
    onEditStatus: (AiringEpisode) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            if (onBack != null) {
                com.owlcoder.animeschedule.presentation.components.AppInlineHeader(
                    title = stringResource(R.string.schedule_section_today),
                    onBack = onBack,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                AppLargeHeader(title = stringResource(R.string.schedule_section_today), modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        items(episodes.sortedBy { it.airingAtEpochSeconds }, key = { it.airingId }) { episode ->
            UpcomingAiringRow(
                episode = episode,
                isLoggedIn = isLoggedIn,
                isIncrementing = episode.malId in pendingIncrementIds,
                onClick = { onAnimeClick(episode) },
                onIncrement = { onIncrementEpisode(episode) },
                onEditStatus = { onEditStatus(episode) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeeAllSheet(
    title: String,
    episodes: List<AiringEpisode>,
    onCardClick: (AiringEpisode) -> Unit,
    onDismiss: () -> Unit,
    pendingIncrementIds: Set<Int>,
    onIncrementEpisode: (AiringEpisode) -> Unit,
    isLoggedIn: Boolean,
    onEditStatus: (AiringEpisode) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(episodes.sortedBy { it.airingAtEpochSeconds }, key = { "all-${it.airingId}" }) { episode ->
                    UpcomingAiringRow(
                        episode = episode,
                        isLoggedIn = isLoggedIn,
                        isIncrementing = episode.malId in pendingIncrementIds,
                        onClick = { onCardClick(episode) },
                        onIncrement = { onIncrementEpisode(episode) },
                        onEditStatus = { onEditStatus(episode) },
                    )
                }
            }
        }
    }
}

private fun airingTimeLabel(episode: AiringEpisode): String =
    java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .toString()
        .take(5)
