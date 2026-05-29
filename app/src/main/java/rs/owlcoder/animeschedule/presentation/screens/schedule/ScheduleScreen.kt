package rs.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.owlcoder.animeschedule.presentation.components.EmptyState
import rs.owlcoder.animeschedule.presentation.components.ErrorBanner
import rs.owlcoder.animeschedule.presentation.components.LoadingShimmer
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Raspored", style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    ScheduleTab.entries.forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.label) }
                        )
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
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
                uiState.isLoading -> LoadingShimmer()
                else -> when (uiState.selectedTab) {
                    ScheduleTab.TODAY -> EpisodeList(
                        episodes = uiState.todayEpisodes,
                        isLoggedIn = uiState.isLoggedIn,
                        onCardClick = { onAnimeClick(it.animeId) },
                        onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) }
                    )
                    ScheduleTab.TOMORROW -> EpisodeList(
                        episodes = uiState.tomorrowEpisodes,
                        isLoggedIn = uiState.isLoggedIn,
                        onCardClick = { onAnimeClick(it.animeId) },
                        onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) }
                    )
                    ScheduleTab.WEEK -> WeekList(
                        scheduleDays = uiState.weekDays,
                        isLoggedIn = uiState.isLoggedIn,
                        onCardClick = { onAnimeClick(it.animeId) },
                        onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<rs.owlcoder.animeschedule.domain.model.AiringEpisode>,
    isLoggedIn: Boolean,
    onCardClick: (rs.owlcoder.animeschedule.domain.model.AiringEpisode) -> Unit,
    onIncrementEpisode: (rs.owlcoder.animeschedule.domain.model.AiringEpisode) -> Unit
) {
    if (episodes.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "Nema epizoda",
            subtitle = "Nema emitovanja za ovaj period"
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(episodes, key = { it.airingId }) { ep ->
            AiringEpisodeCard(
                episode = ep,
                isLoggedIn = isLoggedIn,
                onCardClick = { onCardClick(ep) },
                onIncrementEpisode = { onIncrementEpisode(ep) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale("sr"))

@Composable
private fun WeekList(
    scheduleDays: List<rs.owlcoder.animeschedule.domain.model.ScheduleDay>,
    isLoggedIn: Boolean,
    onCardClick: (rs.owlcoder.animeschedule.domain.model.AiringEpisode) -> Unit,
    onIncrementEpisode: (rs.owlcoder.animeschedule.domain.model.AiringEpisode) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize()) {
        scheduleDays.forEach { day ->
            item(key = day.date.toString()) {
                Text(
                    day.date.format(dayFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            items(day.episodes, key = { it.airingId }) { ep ->
                AiringEpisodeCard(
                    episode = ep,
                    isLoggedIn = isLoggedIn,
                    onCardClick = { onCardClick(ep) },
                    onIncrementEpisode = { onIncrementEpisode(ep) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
