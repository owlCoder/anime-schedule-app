package rs.owlcoder.animeschedule.presentation.screens.schedule

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import rs.owlcoder.animeschedule.R
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.ScheduleDay
import rs.owlcoder.animeschedule.presentation.components.EmptyState
import rs.owlcoder.animeschedule.presentation.components.ErrorBanner
import rs.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import rs.owlcoder.animeschedule.presentation.components.LoadingShimmer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tabIcons: Map<ScheduleTab, ImageVector> = mapOf(
    ScheduleTab.TODAY to Icons.Default.Today,
    ScheduleTab.TOMORROW to Icons.Default.Upcoming,
    ScheduleTab.WEEK to Icons.Default.CalendarViewWeek
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingAnimeId by remember { mutableStateOf<Int?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }

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
                    title = { Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.filter.isActive) Badge()
                                }
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filteri",
                                    tint = if (uiState.filter.isActive)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                val tabLabels = mapOf(
                    ScheduleTab.TODAY to stringResource(R.string.schedule_tab_today),
                    ScheduleTab.TOMORROW to stringResource(R.string.schedule_tab_tomorrow),
                    ScheduleTab.WEEK to stringResource(R.string.schedule_tab_week)
                )
                IosSegmentedTabs(
                    tabs = ScheduleTab.entries,
                    selected = uiState.selectedTab,
                    onSelect = { viewModel.selectTab(it) },
                    icon = { tabIcons[it]!! },
                    label = { tabLabels[it]!! },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                if (uiState.selectedTab == ScheduleTab.WEEK) {
                    WeekDayPicker(
                        weekDays = uiState.weekDays.map { it.date },
                        selected = uiState.selectedWeekDay,
                        onSelect = { viewModel.selectWeekDay(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp)
                    )
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
                        onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) },
                        onEditStatus = { editingAnimeId = it.animeId }
                    )
                    ScheduleTab.TOMORROW -> EpisodeList(
                        episodes = uiState.tomorrowEpisodes,
                        isLoggedIn = uiState.isLoggedIn,
                        onCardClick = { onAnimeClick(it.animeId) },
                        onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) },
                        onEditStatus = { editingAnimeId = it.animeId }
                    )
                    ScheduleTab.WEEK -> {
                        val dayEpisodes = uiState.weekDays
                            .find { it.date == uiState.selectedWeekDay }
                            ?.episodes ?: emptyList()
                        EpisodeList(
                            episodes = dayEpisodes,
                            isLoggedIn = uiState.isLoggedIn,
                            onCardClick = { onAnimeClick(it.animeId) },
                            onIncrementEpisode = { viewModel.incrementEpisode(it.animeId) },
                            onEditStatus = { editingAnimeId = it.animeId }
                        )
                    }
                }
            }
        }
    }

    if (editingAnimeId != null) {
        val ep = when (uiState.selectedTab) {
            ScheduleTab.TODAY -> uiState.todayEpisodes
            ScheduleTab.TOMORROW -> uiState.tomorrowEpisodes
            ScheduleTab.WEEK -> uiState.weekDays.flatMap { it.episodes }
        }.find { it.animeId == editingAnimeId }

        ListStatusBottomSheet(
            animeId = editingAnimeId!!,
            currentEntry = ep?.malListEntry,
            onDismiss = { editingAnimeId = null },
            onConfirm = { animeId, update -> viewModel.updateEntry(animeId, update) }
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
}

@Composable
private fun <T> IosSegmentedTabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    icon: (T) -> ImageVector,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val isSelected = tab == selected
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface
                                  else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "tab_fg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) }
                        )
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = icon(tab),
                            contentDescription = label(tab),
                            modifier = Modifier.size(18.dp),
                            tint = contentColor
                        )
                        Text(
                            text = label(tab),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

private val srLatn = Locale.forLanguageTag("sr-Latn")
private val dayLetterFormatter = DateTimeFormatter.ofPattern("E", srLatn)
private val dayNumberFormatter = DateTimeFormatter.ofPattern("d", srLatn)

@Composable
private fun WeekDayPicker(
    weekDays: List<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { date ->
            val isSelected = date == selected
            val isToday = date == LocalDate.now()
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else if (isToday) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(date) }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = date.format(dayLetterFormatter).uppercase().take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = date.format(dayNumberFormatter),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<AiringEpisode>,
    isLoggedIn: Boolean,
    onCardClick: (AiringEpisode) -> Unit,
    onIncrementEpisode: (AiringEpisode) -> Unit,
    onEditStatus: (AiringEpisode) -> Unit
) {
    if (episodes.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.schedule_empty_title),
            subtitle = stringResource(R.string.schedule_empty_subtitle)
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
                onEditStatus = { onEditStatus(ep) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale("sr"))
