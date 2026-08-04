package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.CountdownText
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.GlassToolbarButton
import com.owlcoder.animeschedule.presentation.components.GlassToolbarGroup
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LoadingShimmer
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.screens.notifications.NotificationsOverlay
import com.owlcoder.animeschedule.presentation.screens.seasonal.SeasonalOverlay
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
    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val selectedEpisodes = uiState.episodesForDate(selectedDate)
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
                    selectedDate = selectedDate,
                    onDateSelected = { selectedEpochDay = it.toEpochDay() },
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

    when (openOverlay) {
        is ScheduleOverlay.Filter -> ScheduleFilterSheet(
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
        is ScheduleOverlay.SeeAll -> SeeAllSheet(
            title = selectedDate.fullDateLabel(),
            episodes = selectedEpisodes,
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
        is ScheduleOverlay.Notifications -> NotificationsOverlay(
            onAnimeClick = onAnimeClick,
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
        )
        is ScheduleOverlay.Seasonal -> SeasonalOverlay(
            onAnimeClick = onAnimeClick,
            onDismiss = { viewModel.setOpenOverlay(ScheduleOverlay.None) },
        )
        ScheduleOverlay.None -> Unit
    }
}

@Composable
private fun TodayHomeContent(
    uiState: ScheduleUiState,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
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
    val today = remember { LocalDate.now() }
    val motion = LocalMotionPolicy.current
    val selectedEpisodes = uiState.episodesForDate(selectedDate).sortedBy { it.airingAtEpochSeconds }
    val isToday = selectedDate == today
    val todaySelection = if (isToday) {
        DashboardScheduleSelector.select(selectedEpisodes, Clock.systemDefaultZone())
    } else null
    val featured = todaySelection?.featured ?: selectedEpisodes.firstOrNull()
    val listEpisodes = todaySelection?.upcoming ?: selectedEpisodes.drop(1).take(5)
    val hasSchedule = selectedEpisodes.isNotEmpty()
    val notificationDescription = if (uiState.unreadNotificationCount > 0) {
        "${uiState.unreadNotificationCount} ${stringResource(R.string.schedule_notifications_action)}"
    } else stringResource(R.string.schedule_notifications_action)

    val sectionTitle = if (!isToday) "Schedule" else when (todaySelection?.mode) {
        DashboardScheduleMode.UPCOMING -> "Next 90 minutes"
        DashboardScheduleMode.LATER_TODAY -> "Later today"
        DashboardScheduleMode.EARLIER_TODAY, null -> "Earlier today"
    }
    val sectionSubtitle = if (!isToday) {
        when (selectedEpisodes.size) {
            0 -> "No broadcasts"
            1 -> "1 broadcast"
            else -> "${selectedEpisodes.size} broadcasts"
        }
    } else when (todaySelection?.mode) {
        DashboardScheduleMode.UPCOMING -> if (listEpisodes.isEmpty()) "No more broadcasts soon" else "${listEpisodes.size} upcoming"
        DashboardScheduleMode.LATER_TODAY -> "${listEpisodes.size + if (featured != null) 1 else 0} broadcasts remaining"
        DashboardScheduleMode.EARLIER_TODAY, null -> "Latest broadcasts from today"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "today-header") {
            AppLargeHeader(
                title = stringResource(R.string.schedule_tab_today),
                subtitle = selectedDate.fullDateLabel(),
                trailingContent = {
                    GlassToolbarGroup {
                        GlassToolbarButton(
                            icon = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.nav_seasonal),
                            onClick = onSeasonal,
                        )
                        GlassToolbarButton(
                            icon = Icons.Default.Notifications,
                            contentDescription = notificationDescription,
                            onClick = onNotifications,
                            selected = uiState.unreadNotificationCount > 0,
                        )
                        GlassToolbarButton(
                            icon = Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.filter_title),
                            onClick = onFilter,
                            selected = uiState.filter.isActive,
                        )
                    }
                },
            )
        }

        item(key = "date-rail") {
            ScheduleDateRail(
                selectedDate = selectedDate,
                dates = remember(today) { (0L..6L).map(today::plusDays) },
                onDateSelected = onDateSelected,
            )
        }

        scheduleError?.let { error -> item(key = "schedule-error") { ErrorBanner(error, onRetry) } }

        item(key = "dashboard-content") {
            AnimatedContent(
                targetState = selectedDate.toEpochDay(),
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    val enter = slideInHorizontally(
                        animationSpec = motion.iosTween(IosMotion.Standard),
                        initialOffsetX = { if (motion.animationsEnabled) direction * it / 10 else 0 },
                    ) + fadeIn(animationSpec = motion.iosTween(IosMotion.Standard))
                    val exit = slideOutHorizontally(
                        animationSpec = motion.iosTween(IosMotion.Quick),
                        targetOffsetX = { if (motion.animationsEnabled) -direction * it / 14 else 0 },
                    ) + fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                    enter togetherWith exit
                },
                label = "schedule-date-content",
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!hasSchedule) {
                        EmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = stringResource(R.string.schedule_empty_title),
                            subtitle = if (isToday) stringResource(R.string.schedule_empty_subtitle)
                            else "Nothing is scheduled for ${selectedDate.shortDateLabel()}.",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            actionLabel = stringResource(R.string.schedule_see_all),
                            onAction = onSeeAll,
                        )
                    } else {
                        featured?.let { episode ->
                            FeaturedAiring(
                                episode = episode,
                                status = if (isToday) featuredStatus(episode) else "First broadcast",
                                isLoggedIn = uiState.isLoggedIn,
                                isIncrementing = episode.malId in uiState.pendingIncrementIds,
                                onClick = { onCardClick(episode) },
                                onIncrement = { onIncrementEpisode(episode) },
                            )
                        }

                        DashboardSectionHeader(
                            title = sectionTitle,
                            subtitle = sectionSubtitle,
                            onSeeAll = onSeeAll,
                        )

                        if (listEpisodes.isNotEmpty()) {
                            UpcomingAiringList(
                                episodes = listEpisodes,
                                isLoggedIn = uiState.isLoggedIn,
                                pendingIncrementIds = uiState.pendingIncrementIds,
                                onCardClick = onCardClick,
                                onIncrementEpisode = onIncrementEpisode,
                                onEditStatus = onEditStatus,
                            )
                        } else {
                            AppMaterialSurface(
                                modifier = Modifier.fillMaxWidth().clickable(onClick = onSeeAll),
                                material = AppMaterial.Grouped,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Nothing else is airing in this window. Open the full day schedule.",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun DashboardSectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onSeeAll,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(stringResource(R.string.schedule_see_all), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(3.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ScheduleDateRail(
    selectedDate: LocalDate,
    dates: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val motion = LocalMotionPolicy.current
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        dates.forEach { date ->
            val isSelected = date == selectedDate
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else Color.Transparent,
                animationSpec = motion.iosTween(IosMotion.Standard),
                label = "date-cell-fill",
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                else Color.Transparent,
                animationSpec = motion.iosTween(IosMotion.Standard),
                label = "date-cell-border",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.97f,
                animationSpec = motion.iosSpring(),
                label = "date-cell-scale",
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable { onDateSelected(date) },
                shape = RoundedCornerShape(13.dp),
                color = containerColor,
                contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(0.5.dp, borderColor),
                tonalElevation = 0.dp,
            ) {
                DateCellContent(date, isSelected)
            }
        }
    }
}

@Composable
private fun DateCellContent(date: LocalDate, selected: Boolean) {
    val motion = LocalMotionPolicy.current
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.iosTween(IosMotion.Standard),
        label = "date-cell-content",
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
        )
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun FeaturedAiring(
    episode: AiringEpisode,
    status: String,
    isLoggedIn: Boolean,
    isIncrementing: Boolean,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
            .animateContentSize(animationSpec = motion.iosSpring())
            .clickable(onClick = onClick),
        material = AppMaterial.Grouped,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MediaThumbnail.Small(
                url = episode.coverImageUrl,
                contentDescription = episode.title,
                modifier = Modifier.size(width = 54.dp, height = 74.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = airingTimeLabel(episode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = stringResource(
                            R.string.schedule_episode_label,
                            episode.episode,
                            episode.totalEpisodes?.let { "/$it" } ?: "",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("·", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CountdownText(episode.airingAtEpochSeconds)
                }
            }
            if (isLoggedIn && episode.malListEntry != null) {
                GlassButton(
                    onClick = onIncrement,
                    enabled = !isIncrementing,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) { color ->
                    if (isIncrementing) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
                    } else {
                        Icon(
                            Icons.Default.Add,
                            stringResource(R.string.schedule_hero_action_watched),
                            Modifier.size(16.dp),
                            tint = color,
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
    val motion = LocalMotionPolicy.current
    AppMaterialSurface(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = motion.iosSpring()),
        material = AppMaterial.Grouped,
        shape = MaterialTheme.shapes.large,
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
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 108.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                airingTimeLabel(episode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            CountdownText(episode.airingAtEpochSeconds)
        }
        MediaThumbnail.Small(
            url = episode.coverImageUrl,
            contentDescription = episode.title,
            modifier = Modifier.size(width = 40.dp, height = 54.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.schedule_episode_label,
                    episode.episode,
                    episode.totalEpisodes?.let { "/$it" } ?: "",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isLoggedIn && episode.malListEntry != null) {
            androidx.compose.material3.IconButton(
                onClick = onIncrement,
                enabled = !isIncrementing,
                modifier = Modifier.size(44.dp),
            ) {
                if (isIncrementing) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Add, stringResource(R.string.schedule_hero_action_watched), Modifier.size(16.dp))
            }
            androidx.compose.material3.IconButton(onClick = onEditStatus, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.ChevronRight, stringResource(R.string.schedule_edit_status_action), Modifier.size(16.dp))
            }
        } else {
            Icon(
                Icons.Default.ChevronRight,
                null,
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
    ) {
        item {
            if (onBack != null) {
                AppInlineHeader(
                    title = stringResource(R.string.schedule_section_today),
                    onBack = onBack,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                AppLargeHeader(
                    title = stringResource(R.string.schedule_section_today),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
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
            HorizontalDivider(
                modifier = Modifier.padding(start = 108.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
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
    AppSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        title = title,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            contentPadding = PaddingValues(bottom = 18.dp),
        ) {
            items(
                items = episodes.sortedBy { it.airingAtEpochSeconds },
                key = { "all-${it.airingId}" },
            ) { episode ->
                UpcomingAiringRow(
                    episode = episode,
                    isLoggedIn = isLoggedIn,
                    isIncrementing = episode.malId in pendingIncrementIds,
                    onClick = { onCardClick(episode) },
                    onIncrement = { onIncrementEpisode(episode) },
                    onEditStatus = { onEditStatus(episode) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 108.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

private fun ScheduleUiState.episodesForDate(date: LocalDate): List<AiringEpisode> {
    val today = LocalDate.now()
    return when (date) {
        today -> todayEpisodes
        today.plusDays(1) -> tomorrowEpisodes
        else -> weekDays.firstOrNull { it.date == date }?.episodes.orEmpty()
    }
}

private fun featuredStatus(episode: AiringEpisode): String {
    val now = System.currentTimeMillis() / 1000L
    return when {
        episode.airingAtEpochSeconds > now -> "Up next"
        now - episode.airingAtEpochSeconds <= 30 * 60 -> "Just aired"
        else -> "Latest today"
    }
}

private fun LocalDate.fullDateLabel(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

private fun LocalDate.shortDateLabel(): String =
    format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

private fun airingTimeLabel(episode: AiringEpisode): String =
    java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .toString()
        .take(5)
