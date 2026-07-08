package com.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AiringEpisode
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.presentation.components.CountdownText
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LoadingShimmer
import com.owlcoder.animeschedule.presentation.screens.notifications.NotificationsOverlay
import com.owlcoder.animeschedule.presentation.screens.seasonal.SeasonalOverlay
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (Int) -> Unit,
    onInitialLoadChange: (Boolean) -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val toast = LocalToast.current

    // Report first-load state up so the host can cover the whole app (incl. nav bar) with the
    // animated splash until the very first schedule load resolves.
    LaunchedEffect(uiState.isInitialLoad) { onInitialLoadChange(uiState.isInitialLoad) }
    var editingEpisode by remember { mutableStateOf<AiringEpisode?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSeasonal by remember { mutableStateOf(false) }
    var seeAllSection by remember { mutableStateOf<Pair<String, List<AiringEpisode>>?>(null) }
    // Tracks the episode behind the most recent "+1" tap so that, if it just landed on the
    // series finale, the incrementEvent handler below can pop the same status/score sheet
    // used for manual edits — pre-filled to prompt a rating + a switch to Completed.
    var lastIncrementedEpisode by remember { mutableStateOf<AiringEpisode?>(null) }
    val markedMsg = stringResource(R.string.toast_episode_marked)
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)

    val scheduleErrorMsg = uiState.errorRes?.let { stringResource(it) }
    LaunchedEffect(scheduleErrorMsg) {
        scheduleErrorMsg?.let { toast.error(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.incrementEvent.collect { event ->
            when (event) {
                is ScheduleViewModel.IncrementEvent.Success -> {
                    toast.success(markedMsg)
                    val ep = lastIncrementedEpisode
                    val entry = ep?.malListEntry
                    val total = entry?.totalEpisodes ?: ep?.totalEpisodes
                    if (ep != null && entry != null && total != null &&
                        entry.episodesWatched + 1 >= total
                    ) {
                        // The "+1" that just landed hit the finale — prompt the same status
                        // sheet used for manual edits, pre-filled with the just-watched count
                        // and status already nudged to Completed (before uiState re-emits with
                        // the fresh DB value, which the sheet's own snapshot-on-open otherwise
                        // wouldn't see for a beat).
                        editingEpisode = ep.copy(
                            malListEntry = entry.copy(
                                episodesWatched = total,
                                status = com.owlcoder.animeschedule.domain.model.WatchStatus.COMPLETED
                            )
                        )
                    }
                }
                is ScheduleViewModel.IncrementEvent.Updated ->
                    toast.success(savedMsg)
                is ScheduleViewModel.IncrementEvent.Removed ->
                    toast.success(removedMsg)
                is ScheduleViewModel.IncrementEvent.Error ->
                    toast.error(errorMsg)
            }
            lastIncrementedEpisode = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        TopBarActionButton(
                            onClick = { showSeasonal = true },
                            contentDescription = stringResource(R.string.nav_seasonal)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TopBarActionButton(
                            onClick = { showNotifications = true },
                            contentDescription = stringResource(R.string.schedule_notifications_action)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (uiState.unreadNotificationCount > 0) {
                                        val label = if (uiState.unreadNotificationCount > 9) "9+"
                                                    else uiState.unreadNotificationCount.toString()
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (-4).dp, y = 4.dp)
                                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TopBarActionButton(
                            onClick = { showFilterSheet = true },
                            contentDescription = stringResource(R.string.filter_title),
                            highlighted = uiState.filter.isActive
                        ) {
                            BadgedBox(
                                badge = {
                                    if (uiState.filter.isActive) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = if (uiState.filter.isActive)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(innerPadding)
        ) {
            when {
                // First-ever load is covered by the full-screen animated splash (hosted above),
                // so render nothing here. A pull-to-refresh with no content yet falls back to the
                // skeleton; otherwise show the (possibly stale) content while refreshing.
                uiState.isInitialLoad -> Unit
                uiState.isLoading && uiState.todayEpisodes.isEmpty() &&
                    uiState.tomorrowEpisodes.isEmpty() && uiState.weekDays.isEmpty() -> LoadingShimmer()
                else -> TodayHomeContent(
                    uiState = uiState,
                    onCardClick = { onAnimeClick(it.animeId) },
                    onIncrementEpisode = { ep ->
                        ep.malId?.let { mid ->
                            lastIncrementedEpisode = ep
                            viewModel.incrementEpisode(mid)
                        }
                    },
                    onEditStatus = { editingEpisode = it },
                    onSeeAll = { title, episodes -> seeAllSection = title to episodes },
                    onRecentEntryClick = { onAnimeClick(it.animeId) },
                    onIncrementEntry = { entry -> viewModel.incrementEpisode(entry.animeId) }
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
            onRemove = { id -> viewModel.removeEntry(id) }
        )
    }

    if (showFilterSheet) {
        ScheduleFilterSheet(
            filter = uiState.filter,
            availableGenres = uiState.availableGenres,
            availableFormats = uiState.availableFormats,
            isLoggedIn = uiState.isLoggedIn,
            onOnlyMyListChange = { viewModel.setOnlyMyList(it) },
            onGenreToggle = { viewModel.toggleGenre(it) },
            onFormatToggle = { viewModel.toggleFormat(it) },
            onClear = { viewModel.clearFilter() },
            onDismiss = { showFilterSheet = false }
        )
    }

    seeAllSection?.let { (title, episodes) ->
        SeeAllSheet(
            title = title,
            episodes = episodes,
            onCardClick = { seeAllSection = null; onAnimeClick(it.animeId) },
            onDismiss = { seeAllSection = null },
            pendingIncrementIds = uiState.pendingIncrementIds,
            onIncrementEpisode = { ep ->
                ep.malId?.let { mid ->
                    lastIncrementedEpisode = ep
                    viewModel.incrementEpisode(mid)
                }
            }
        )
    }

    if (showNotifications) {
        NotificationsOverlay(
            onAnimeClick = { showNotifications = false; onAnimeClick(it) },
            onDismiss = { showNotifications = false }
        )
    }

    if (showSeasonal) {
        SeasonalOverlay(
            onAnimeClick = { showSeasonal = false; onAnimeClick(it) },
            onDismiss = { showSeasonal = false }
        )
    }
}

/**
 * "See all" bottom sheet: shows every episode of a section (Today / Tomorrow / This week)
 * as a 3-column grid, instead of the horizontally-scrolling preview strip on the home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeeAllSheet(
    title: String,
    episodes: List<AiringEpisode>,
    onCardClick: (AiringEpisode) -> Unit,
    onDismiss: () -> Unit,
    pendingIncrementIds: Set<Int> = emptySet(),
    onIncrementEpisode: ((AiringEpisode) -> Unit)? = null
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                gridItems(episodes, key = { it.airingId }) { ep ->
                    MiniEpisodeCard(
                        episode = ep,
                        onClick = { onCardClick(ep) },
                        modifier = Modifier.fillMaxWidth(),
                        isIncrementing = ep.malId in pendingIncrementIds,
                        onIncrementEpisode = onIncrementEpisode?.let { { it(ep) } }
                    )
                }
            }
        }
    }
}

/**
 * Top bar action icon wrapped in a subtle circular surface so the three actions
 * (Seasonal / Notifications / Filter) read as one consistent icon-button set
 * instead of bare icons floating in the app bar.
 */
@Composable
private fun TopBarActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val backgroundColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    }
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .semantics { this.contentDescription = contentDescription }
    ) {
        content()
    }
}

/**
 * Schedule home content: a rotating hero pager for the first few airing-today
 * entries, followed by horizontal sections for Today / Tomorrow / This week,
 * all visible in one continuous vertical scroll (no tab/segment selector).
 */
@Composable
private fun TodayHomeContent(
    uiState: ScheduleUiState,
    onCardClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit,
    onEditStatus: (AiringEpisode) -> Unit,
    onSeeAll: (title: String, episodes: List<AiringEpisode>) -> Unit,
    onRecentEntryClick: (MalListEntry) -> Unit = {},
    onIncrementEntry: (MalListEntry) -> Unit = {}
) {
    val heroEpisodes = (uiState.todayEpisodes.ifEmpty { uiState.tomorrowEpisodes }).take(5)
    val weekEpisodes = uiState.weekDays.flatMap { it.episodes }
    val todayTitle = stringResource(R.string.schedule_section_today)
    val tomorrowTitle = stringResource(R.string.schedule_section_tomorrow)
    val recentlyChangedTitle = stringResource(R.string.schedule_section_recently_changed)
    val weekTitle = stringResource(R.string.schedule_section_this_week)

    if (heroEpisodes.isEmpty() && uiState.todayEpisodes.isEmpty() && uiState.tomorrowEpisodes.isEmpty() && weekEpisodes.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.schedule_empty_title),
            subtitle = stringResource(R.string.schedule_empty_subtitle)
        )
        return
    }

    val navBarHeight = LocalNavBarHeight.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        if (heroEpisodes.isNotEmpty()) {
            item {
                HeroPagerSection(
                    episodes = heroEpisodes,
                    isLoggedIn = uiState.isLoggedIn,
                    pendingIncrementIds = uiState.pendingIncrementIds,
                    onCardClick = onCardClick,
                    onIncrementEpisode = onIncrementEpisode
                )
            }
        }
        if (uiState.todayEpisodes.isNotEmpty()) {
            item {
                HorizontalEpisodeSection(
                    title = todayTitle,
                    episodes = uiState.todayEpisodes,
                    onCardClick = onCardClick,
                    onSeeAll = { onSeeAll(todayTitle, uiState.todayEpisodes) },
                    pendingIncrementIds = uiState.pendingIncrementIds,
                    onIncrementEpisode = onIncrementEpisode
                )
            }
        }
        if (uiState.tomorrowEpisodes.isNotEmpty()) {
            item {
                HorizontalEpisodeSection(
                    title = tomorrowTitle,
                    episodes = uiState.tomorrowEpisodes,
                    onCardClick = onCardClick,
                    onSeeAll = { onSeeAll(tomorrowTitle, uiState.tomorrowEpisodes) },
                    pendingIncrementIds = uiState.pendingIncrementIds,
                    onIncrementEpisode = onIncrementEpisode
                )
            }
        }
        if (uiState.recentlyChangedEntries.isNotEmpty()) {
            item {
                RecentlyChangedSection(
                    title = recentlyChangedTitle,
                    entries = uiState.recentlyChangedEntries,
                    pendingIncrementIds = uiState.pendingIncrementIds,
                    onCardClick = onRecentEntryClick,
                    onIncrementEntry = onIncrementEntry
                )
            }
        }
        if (weekEpisodes.isNotEmpty()) {
            item {
                HorizontalEpisodeSection(
                    title = weekTitle,
                    episodes = weekEpisodes,
                    onCardClick = onCardClick,
                    onSeeAll = { onSeeAll(weekTitle, weekEpisodes) },
                    pendingIncrementIds = uiState.pendingIncrementIds,
                    onIncrementEpisode = onIncrementEpisode
                )
            }
        }
        item { Spacer(Modifier.height(navBarHeight)) }
    }
}

@Composable
private fun HeroPagerSection(
    episodes: List<AiringEpisode>,
    isLoggedIn: Boolean,
    pendingIncrementIds: Set<Int>,
    onCardClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { episodes.size })
    // Auto-advance the hero carousel every few seconds so it feels alive; pauses
    // naturally while the user is dragging (isScrollInProgress) and resumes after.
    if (episodes.size > 1) {
        LaunchedEffect(pagerState, episodes.size) {
            while (true) {
                kotlinx.coroutines.delay(4500)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % episodes.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val episode = episodes[page]
            HeroCard(
                episode = episode,
                isLoggedIn = isLoggedIn,
                isIncrementing = episode.malId != null && episode.malId in pendingIncrementIds,
                onCardClick = { onCardClick(episode) },
                onIncrementEpisode = { onIncrementEpisode(episode) }
            )
        }
        if (episodes.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(episodes.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 20.dp else 6.dp)
                            .clip(PillShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    episode: AiringEpisode,
    isLoggedIn: Boolean,
    isIncrementing: Boolean,
    onCardClick: () -> Unit,
    onIncrementEpisode: () -> Unit
) {
    val hasListEntry = episode.malListEntry != null
    val showWatchedAction = isLoggedIn && hasListEntry

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onCardClick)
    ) {
        // Sharp cover art fills the whole card.
        AsyncImage(
            model = episode.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Consistent real blur on EVERY card: a full-size blurred copy of the cover, revealed
        // only over the bottom ~55% via a vertical alpha-gradient mask. The mask is drawn with
        // BlendMode.DstIn inside an offscreen-composited layer (graphicsLayer), so the blurred
        // image itself fades out toward the top — no fragile fixed-height crop math, so the
        // softened band lands in the same place on every card regardless of artwork.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.62f to Color.Black,
                                1.0f to Color.Black
                            )
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
        ) {
            AsyncImage(
                model = episode.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        }
        // Darkening + scrim so title/meta/button read clearly over the (now blurred) bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.18f),
                            1.0f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.LiveTv,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                val total = episode.totalEpisodes?.let { "/$it" } ?: ""
                Text(
                    text = stringResource(R.string.schedule_episode_label, episode.episode, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                if (java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds).isAfter(java.time.Instant.now())) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    CountdownText(
                        airingAtEpochSeconds = episode.airingAtEpochSeconds,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            // Glass CTA over imagery — reusable GlassButton with the white-sheen "onImagery"
            // variant so it reads as frosted glass sitting on the hero art.
            val ctaEnabled = !showWatchedAction || !isIncrementing
            GlassButton(
                onClick = { if (showWatchedAction) onIncrementEpisode() else onCardClick() },
                enabled = ctaEnabled,
                onImagery = true,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) { contentColor ->
                if (showWatchedAction) {
                    if (isIncrementing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                    }
                    Text(
                        text = stringResource(R.string.schedule_hero_action_watched),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = contentColor
                    )
                    Text(
                        text = stringResource(R.string.schedule_hero_action_details),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalEpisodeSection(
    title: String,
    episodes: List<AiringEpisode>,
    onCardClick: (AiringEpisode) -> Unit,
    onSeeAll: () -> Unit,
    pendingIncrementIds: Set<Int> = emptySet(),
    onIncrementEpisode: ((AiringEpisode) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(episodes, key = { it.airingId }) { ep ->
                MiniEpisodeCard(
                    episode = ep,
                    onClick = { onCardClick(ep) },
                    isIncrementing = ep.malId in pendingIncrementIds,
                    onIncrementEpisode = onIncrementEpisode?.let { { it(ep) } }
                )
            }
        }
    }
}

@Composable
private fun MiniEpisodeCard(
    episode: AiringEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(140.dp),
    isIncrementing: Boolean = false,
    onIncrementEpisode: (() -> Unit)? = null
) {
    // Note: only the cover image is clipped to a rounded shape — the whole Column is NOT
    // clipped, otherwise the rounded corners crop the first/last glyph of the text labels
    // below (which was cutting off the "Ep." / title on the left edge).
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            AsyncImage(
                model = episode.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.medium)
            )
            // Accent countdown pill, floating over the top-left corner of the cover — only
            // while the episode hasn't aired yet; once it has, the pill carries no useful
            // info so it's hidden entirely instead of showing a stale "Emitovano" label.
            if (java.time.Instant.ofEpochSecond(episode.airingAtEpochSeconds).isAfter(java.time.Instant.now())) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    CountdownText(
                        airingAtEpochSeconds = episode.airingAtEpochSeconds,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            // Airing episode number, top-right — which episode this card is for, at a
            // glance, independent of the user's own watch progress shown below.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_episode_number_short, episode.episode),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = episode.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        val total = episode.totalEpisodes?.let { "/$it" } ?: ""
        Text(
            modifier = Modifier.padding(horizontal = 2.dp),
            text = stringResource(R.string.schedule_episode_label, episode.episode, total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Watch-progress bar + quick "+1" action, only for anime already on the user's list.
        episode.malListEntry?.let { entry ->
            val watchedTotal = entry.totalEpisodes ?: episode.totalEpisodes
            val progress = watchedTotal?.takeIf { it > 0 }
                ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
            Column(
                modifier = Modifier.padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${entry.episodesWatched}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    if (watchedTotal != null) {
                        Text(
                            text = "$watchedTotal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (progress != null) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(PillShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            strokeCap = androidx.compose.material3.ProgressIndicatorDefaults.LinearStrokeCap
                        )
                    }
                    if (onIncrementEpisode != null) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(enabled = !isIncrementing, onClick = onIncrementEpisode),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isIncrementing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.schedule_hero_action_watched),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Recently changed" section: MAL Watching-status titles sorted by most-recently-updated,
 * surfaced on the Schedule home even when they're not airing today/tomorrow — a recent
 * progress edit is a strong signal the user is actively watching that title right now.
 */
@Composable
private fun RecentlyChangedSection(
    title: String,
    entries: List<MalListEntry>,
    pendingIncrementIds: Set<Int>,
    onCardClick: (MalListEntry) -> Unit,
    onIncrementEntry: (MalListEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(entries, key = { it.animeId }) { entry ->
                RecentEntryCard(
                    entry = entry,
                    onClick = { onCardClick(entry) },
                    isIncrementing = entry.animeId in pendingIncrementIds,
                    onIncrementEpisode = { onIncrementEntry(entry) }
                )
            }
        }
    }
}

@Composable
private fun RecentEntryCard(
    entry: MalListEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(140.dp),
    isIncrementing: Boolean = false,
    onIncrementEpisode: () -> Unit = {}
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = entry.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
        )
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        val total = entry.totalEpisodes
        val progress = total?.takeIf { it > 0 }
            ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        Column(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${entry.episodesWatched}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                if (total != null) {
                    Text(
                        text = "$total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (progress != null) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(PillShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        strokeCap = androidx.compose.material3.ProgressIndicatorDefaults.LinearStrokeCap
                    )
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = !isIncrementing, onClick = onIncrementEpisode),
                    contentAlignment = Alignment.Center
                ) {
                    if (isIncrementing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.schedule_hero_action_watched),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

