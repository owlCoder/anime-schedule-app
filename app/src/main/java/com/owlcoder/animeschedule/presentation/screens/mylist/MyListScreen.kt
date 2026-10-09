package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.domain.model.SmartListFilter
import com.owlcoder.animeschedule.domain.model.minutesFor
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.AppChoiceRow
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.GlassToolbarGroup
import com.owlcoder.animeschedule.presentation.components.GlassToolbarButton
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppErrorState
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppLoadingState
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSearchField
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.components.labelRes
import com.owlcoder.animeschedule.presentation.components.contentTransform
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

private enum class ListOverlay { SORT, INSIGHTS, TOOLS, HISTORY, PICK, TAGS, SMART, VIEWS, PLANNER, CALENDAR, RATING, BULK, MANAGE_TAGS, BACKLOG, COMPARE }

private val statusTabs = listOf(
    WatchStatus.WATCHING,
    WatchStatus.COMPLETED,
    WatchStatus.PLAN_TO_WATCH,
    WatchStatus.ON_HOLD,
    WatchStatus.DROPPED,
)

private fun WatchStatus.tabIcon(selected: Boolean): ImageVector = when (this) {
    WatchStatus.WATCHING -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle
    WatchStatus.COMPLETED -> if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle
    WatchStatus.PLAN_TO_WATCH -> if (selected) Icons.Filled.Bookmark else Icons.Outlined.Bookmark
    WatchStatus.ON_HOLD -> if (selected) Icons.Filled.PauseCircle else Icons.Outlined.PauseCircle
    WatchStatus.DROPPED -> if (selected) Icons.Filled.RemoveCircle else Icons.Outlined.RemoveCircle
    WatchStatus.NOT_IN_LIST -> Icons.Outlined.Bookmark
}

@Composable
fun MyListScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: MyListViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    initialTool: com.owlcoder.animeschedule.domain.model.ToolShortcut? = null,
    onToolOpened: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingEntry by remember { mutableStateOf<MalListEntry?>(null) }
    var overlay by remember { mutableStateOf<ListOverlay?>(null) }
    LaunchedEffect(initialTool) {
        when (initialTool) {
            com.owlcoder.animeschedule.domain.model.ToolShortcut.PLANNER -> overlay = ListOverlay.PLANNER
            com.owlcoder.animeschedule.domain.model.ToolShortcut.HISTORY -> overlay = ListOverlay.HISTORY
            com.owlcoder.animeschedule.domain.model.ToolShortcut.CALENDAR -> overlay = ListOverlay.CALENDAR
            com.owlcoder.animeschedule.domain.model.ToolShortcut.FAVORITES -> { overlay = null; viewModel.showFavorites() }
            else -> Unit
        }
        if (initialTool != null) onToolOpened()
    }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val toast = LocalToast.current
    val motion = LocalMotionPolicy.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)
    val totalCount = uiState.statusCounts.values.sum()
    val scope = rememberCoroutineScope()
    var exportSnapshot by remember { mutableStateOf("") }
    var pickedEntry by remember { mutableStateOf<MalListEntry?>(null) }
    val exportedMsg = stringResource(R.string.list_exported)
    val exportErrorMsg = stringResource(R.string.list_export_error)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = context.contentResolver.openOutputStream(uri) ?: error("No output stream")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(exportSnapshot) }
                }.isSuccess
            }
            if (success) toast.success(exportedMsg) else toast.error(exportErrorMsg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                is MyListViewModel.UpdateEvent.Success -> {
                    showError = false
                    toast.success(savedMsg)
                }
                is MyListViewModel.UpdateEvent.PersonalSaved -> toast.success(resources.getString(R.string.bulk_saved))
                is MyListViewModel.UpdateEvent.Removed -> {
                    showError = false
                    toast.success(removedMsg)
                }
                is MyListViewModel.UpdateEvent.Error -> {
                    showError = true
                    toast.error(errorMsg)
                }
            }
        }
    }

    AnimatedContent(
        targetState = uiState.isLoggedIn,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { motion.contentTransform(durationMillis = IosMotion.Quick) },
        label = "my-list-auth-state",
    ) { loggedIn ->
        if (!loggedIn) {
            NotLoggedInState(onLogin = { authViewModel.launchMalLogin(context) })
        } else {
            LoggedInList(
                uiState = uiState,
                totalCount = totalCount,
                showError = showError,
                errorMsg = errorMsg,
                onRefresh = {
                    showError = false
                    viewModel.refresh()
                },
                onSearchQueryChange = viewModel::setSearchQuery,
                onFilterSelected = viewModel::setFilter,
                onAnimeClick = onAnimeClick,
                onIncrementEpisode = viewModel::incrementEpisode,
                onEditStatus = { id -> editingEntry = uiState.entries.find { it.animeId == id } },
                onShowSort = { overlay = ListOverlay.SORT },
                onShowInsights = { overlay = ListOverlay.INSIGHTS },
                onShowTools = { overlay = ListOverlay.TOOLS },
                onFavorites = viewModel::toggleFavorites,
                onUnrated = viewModel::toggleUnrated,
                onTags = { overlay = ListOverlay.TAGS },
                onSmart = { overlay = ListOverlay.SMART },
                onViews = { overlay = ListOverlay.VIEWS },
                onRating = { overlay = ListOverlay.RATING },
                onClearQuickFilters = { viewModel.clearQuickFilters(); viewModel.setFilter(null) },
            )
        }
    }

    editingEntry?.let { entry ->
        ListStatusBottomSheet(
            animeId = entry.animeId,
            currentEntry = entry,
            onDismiss = { editingEntry = null },
            onConfirm = { id, update -> viewModel.updateEntry(id, update) },
            onRemove = { id -> viewModel.removeEntry(id) },
        )
    }
    when (overlay) {
        ListOverlay.SORT, ListOverlay.INSIGHTS -> MyListOptionsSheet(
            overlay = overlay!!, uiState = uiState,
            onSortSelected = { viewModel.setSortOrder(it); overlay = null },
            onDismiss = { overlay = null },
        )
        ListOverlay.TOOLS -> ListToolsSheet(
            canPick = uiState.entries.any { it.status != WatchStatus.COMPLETED && it.status != WatchStatus.DROPPED },
            canExport = uiState.allEntries.isNotEmpty(),
            continueTitle = uiState.allEntries.continueWatching()?.title,
            onContinue = { uiState.allEntries.continueWatching()?.let { overlay = null; onAnimeClick(it.animeId) } },
            onHistory = { overlay = ListOverlay.HISTORY },
            onRating = { overlay = ListOverlay.RATING },
            onBulk = { overlay = ListOverlay.BULK },
            onManageTags = { overlay = ListOverlay.MANAGE_TAGS },
            onBacklog = { overlay = ListOverlay.BACKLOG },
            onCompare = { overlay = ListOverlay.COMPARE },
            onViews = { overlay = ListOverlay.VIEWS },
            onPlanner = { overlay = ListOverlay.PLANNER },
            onCalendar = { overlay = ListOverlay.CALENDAR },
            canShare = uiState.entries.isNotEmpty(),
            onShare = {
                val payload = uiState.entries.toListShareText(
                    resources.getQuantityString(R.plurals.list_share_title_count, uiState.entries.size, uiState.entries.size),
                    line = { e -> resources.getString(R.string.list_share_line, e.title.ifBlank { "#${e.animeId}" }, resources.getString(e.status.labelRes()), e.episodesWatched, e.totalEpisodes?.takeIf { it > 0 }?.toString() ?: "?", e.score) },
                    overflow = { resources.getString(R.string.list_share_more, it) },
                )
                overlay = null
                runCatching {
                    context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, payload)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, resources.getString(R.string.mylist_title))
                    }, resources.getString(R.string.list_share)))
                }.onFailure { toast.error(resources.getString(R.string.list_share_failed)) }
            },
            onPick = {
                pickedEntry = uiState.entries.filter { it.status != WatchStatus.COMPLETED && it.status != WatchStatus.DROPPED }.randomOrNull()
                overlay = ListOverlay.PICK
            },
            onExport = {
                exportSnapshot = uiState.allEntries.toListCsv(uiState.tools)
                overlay = null
                exporter.launch("anime-list-${java.time.LocalDate.now()}.csv")
            }, onDismiss = { overlay = null },
        )
        ListOverlay.HISTORY -> WatchHistorySheet(uiState.tools, viewModel::setWeeklyGoal, viewModel::clearActivity, { overlay = null }, onCalendar = { overlay = ListOverlay.CALENDAR }, onExport = { activities ->
            exportSnapshot = activities.toActivityCsv(); overlay = null
            exporter.launch("anime-activity-${java.time.LocalDate.now()}.csv")
        })
        ListOverlay.RATING -> RatingRangeSheet(uiState.scoreRange, { min, max -> viewModel.setScoreRange(min, max); overlay = null }, { overlay = null })
        ListOverlay.BULK -> BulkOrganizeSheet(uiState.entries, { ids, favorite, pin -> viewModel.setMarkers(ids, favorite, pin); overlay = null }, { overlay = null })
        ListOverlay.MANAGE_TAGS -> ManageTagsSheet(uiState.tools, viewModel::renameTag, { overlay = null })
        ListOverlay.BACKLOG -> BacklogSheet(uiState.allEntries, uiState.tools, { overlay = null })
        ListOverlay.COMPARE -> CompareAnimeSheet(uiState.allEntries.sortedFor(MyListSortOrder.TITLE), uiState.tools, { overlay = null })
        ListOverlay.SMART -> SmartFiltersSheet(uiState.smartFilter, { viewModel.setSmartFilter(it); overlay = null }, { overlay = null })
        ListOverlay.VIEWS -> SavedViewsSheet(uiState.tools.savedViews, viewModel::saveView, { viewModel.applyView(it); overlay = null }, viewModel::deleteView, { overlay = null }, viewModel::renameView, viewModel::moveView)
        ListOverlay.PLANNER -> WatchPlannerSheet(uiState.allEntries, uiState.tools, { overlay = null; onAnimeClick(it) }, { overlay = null })
        ListOverlay.CALENDAR -> ActivityCalendarSheet(uiState.tools, viewModel::setDailyGoal, { overlay = null })
        ListOverlay.PICK -> AnimePickSheet(pickedEntry, onOpen = { entry -> overlay = null; onAnimeClick(entry.animeId) }, onReroll = {
            val candidates = uiState.entries.filter { it.status != WatchStatus.COMPLETED && it.status != WatchStatus.DROPPED && it.animeId != pickedEntry?.animeId }
            pickedEntry = candidates.randomOrNull() ?: pickedEntry
        }, onDismiss = { overlay = null })
        ListOverlay.TAGS -> TagFilterSheet(
            tags = uiState.allEntries.flatMap { uiState.tools.tags[it.animeId].orEmpty() }.distinctBy { it.lowercase(java.util.Locale.ROOT) }.sortedBy { it.lowercase(java.util.Locale.ROOT) },
            selected = uiState.activeTag,
            onSelect = { viewModel.setTagFilter(it); overlay = null }, onDismiss = { overlay = null },
        )
        null -> Unit
    }
}

@Composable
internal fun LoggedInList(
    uiState: MyListUiState,
    totalCount: Int,
    showError: Boolean,
    errorMsg: String,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (WatchStatus?) -> Unit,
    onAnimeClick: (Int) -> Unit,
    onIncrementEpisode: (Int) -> Unit,
    onEditStatus: (Int) -> Unit,
    onShowSort: () -> Unit,
    onShowInsights: () -> Unit,
    onShowTools: () -> Unit,
    onFavorites: () -> Unit,
    onUnrated: () -> Unit,
    onTags: () -> Unit,
    onSmart: () -> Unit,
    onViews: () -> Unit,
    onRating: () -> Unit,
    onClearQuickFilters: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    var showFilters by remember { mutableStateOf(false) }
    val activeCount = listOf(uiState.searchQuery.isNotBlank(), uiState.favoritesOnly,
        uiState.unratedOnly, uiState.activeTag != null, uiState.smartFilter != SmartListFilter.ALL,
        uiState.scoreRange != (0 to 10)).count { it }
    val hasFilters = activeCount > 0 || (uiState.activeFilter != null && uiState.allEntries.isNotEmpty())
    if (showFilters) MyListFiltersSheet(
        uiState = uiState,
        onFavorites = onFavorites, onUnrated = onUnrated,
        onSmart = { showFilters = false; onSmart() },
        onRating = { showFilters = false; onRating() },
        onTags = { showFilters = false; onTags() },
        onViews = { showFilters = false; onViews() },
        onClear = onClearQuickFilters, onDismiss = { showFilters = false },
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLargeHeader(
                        title = stringResource(R.string.mylist_title),
                        subtitle = totalCount.takeIf { it > 0 }?.let { stringResource(R.string.mylist_anime_count, it) },
                        modifier = Modifier.weight(1f),
                    )
                    GlassToolbarGroup {
                        GlassToolbarButton(Icons.Default.BarChart, stringResource(R.string.mylist_statistics), onShowInsights)
                        GlassToolbarButton(Icons.Default.MoreHoriz, stringResource(R.string.list_tools), onShowTools)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSearchField(
                        value = uiState.searchQuery, onValueChange = onSearchQueryChange,
                        placeholder = stringResource(R.string.mylist_search_placeholder),
                        leadingIcon = Icons.Default.Search, onClear = { onSearchQueryChange("") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    )
                    GlassIconButton(Icons.Default.Tune,
                        pluralStringResource(R.plurals.list_filters_active, activeCount, activeCount),
                        { showFilters = true }, Modifier.testTag("list-filter-menu"))
                }
                StatusFilterRow(uiState.activeFilter, uiState.statusCounts, onFilterSelected,
                    Modifier.padding(bottom = 4.dp))
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(uiState.activeFilter?.displayName() ?: stringResource(R.string.list_all_statuses),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.visible_results, uiState.entries.size) + " · " + stringResource(uiState.sortOrder.labelRes),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (activeCount > 0) GlassIconButton(Icons.Default.FilterAltOff,
                        stringResource(R.string.list_clear_filters), onClearQuickFilters, Modifier.testTag("list-clear-active"))
                    GlassIconButton(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.mylist_sort), onShowSort)
                }
                val contentMode = when {
                    uiState.isLoading && uiState.entries.isEmpty() -> 0
                    showError && uiState.entries.isEmpty() -> 1
                    uiState.entries.isEmpty() -> 2
                    else -> 3
                }
                AnimatedContent(
                    targetState = contentMode,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = { motion.contentTransform() },
                    label = "my-list-content",
                ) { mode ->
                    when (mode) {
                        0 -> AppLoadingState(
                            modifier = Modifier.fillMaxSize(),
                            label = stringResource(R.string.mylist_title),
                        )
                        1 -> AppErrorState(
                            title = errorMsg,
                            retryLabel = stringResource(R.string.common_retry),
                            onRetry = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                        )
                        2 -> EmptyState(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            title = stringResource(if (hasFilters) R.string.list_filtered_empty else R.string.mylist_empty_title),
                            subtitle = stringResource(if (hasFilters) R.string.list_filtered_empty_hint else R.string.mylist_empty_subtitle),
                            actionLabel = if (hasFilters) stringResource(R.string.list_clear_filters) else null,
                            onAction = if (hasFilters) onClearQuickFilters else null,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 5.dp, bottom = 116.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (showError) {
                                item(key = "sync-error", contentType = "error") {
                                    ErrorBanner(
                                        message = errorMsg,
                                        onRetry = onRefresh,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                            items(
                                items = uiState.entries,
                                key = { entry -> entry.animeId },
                                contentType = { "my-list-entry" },
                            ) { entry ->
                                MyListEntryCard(
                                    entry = entry,
                                    title = entry.title.ifEmpty { entry.animeId.toString() },
                                    coverImageUrl = entry.coverImageUrl,
                                    isIncrementing = entry.animeId in uiState.pendingIncrementIds,
                                    isFavorite = entry.animeId in uiState.tools.favorites,
                                    isPinned = entry.animeId in uiState.tools.pinned,
                                    hasNote = uiState.tools.notes[entry.animeId]?.isNotBlank() == true,
                                    tags = uiState.tools.tags[entry.animeId].orEmpty(),
                                    remainingMinutes = entry.totalEpisodes?.takeIf { it > 0 }?.let { (it - entry.episodesWatched).coerceAtLeast(0).toLong() * uiState.tools.minutesFor(entry.animeId) },
                                    onCardClick = { onAnimeClick(entry.animeId) },
                                    onIncrementEpisode = { onIncrementEpisode(entry.animeId) },
                                    onEditStatus = { onEditStatus(entry.animeId) },
                                    showDivider = false,
                                    modifier = Modifier.padding(horizontal = 16.dp).testTag("mylist-entry-${entry.animeId}"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    activeFilter: WatchStatus?,
    counts: Map<WatchStatus, Int>,
    onFilterSelected: (WatchStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionPolicy.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        (listOf(null) + statusTabs).forEach { status ->
            val isSelected = status == activeFilter
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = motion.iosTween(IosMotion.Standard),
                label = "my-list-tab-color",
            )
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface)
                else MaterialTheme.colorScheme.surface,
                animationSpec = motion.iosTween(IosMotion.Standard),
                label = "my-list-tab-fill",
            )
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .clip(PillShape)
                    .testTag("list-status-${status?.name ?: "ALL"}")
                    .selectable(isSelected, interactionSource = interactionSource, indication = null,
                        role = Role.Tab, onClick = { onFilterSelected(status) })
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = PillShape,
                    color = containerColor,
                    contentColor = contentColor,
                    border = BorderStroke(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.iosPressScale(interactionSource).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = { motion.contentTransform(durationMillis = IosMotion.Quick) },
                            label = "my-list-tab-icon",
                        ) { selectedState ->
                            Icon(
                                status?.tabIcon(selectedState) ?: Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor,
                            )
                        }
                        Text(
                            text = status?.displayName() ?: stringResource(R.string.list_all_statuses),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                        )
                        (if (status == null) counts.values.sum() else counts[status])?.takeIf { it > 0 }?.let { count ->
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotLoggedInState(onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        AppLargeHeader(stringResource(R.string.mylist_title), Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        LazyColumn(
            Modifier.fillMaxSize().testTag("mylist-sign-in"),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Surface(shape = ContinuousRoundedShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bookmark, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mylist_not_logged_in_title), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.mylist_not_logged_in_subtitle), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            item {
                AppButton(stringResource(R.string.profile_login), onLogin,
                    Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                    variant = AppButtonVariant.Primary, icon = Icons.AutoMirrored.Filled.Login)
            }
            item {
                AppMaterialSurface(Modifier.widthIn(max = 420.dp).fillMaxWidth(), material = AppMaterial.Grouped,
                    shape = ContinuousRoundedShape(20.dp)) {
                    Column {
                        BenefitRow(Icons.Default.CloudSync, stringResource(R.string.mylist_benefit_sync_title), stringResource(R.string.mylist_benefit_sync_subtitle))
                        HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        BenefitRow(Icons.Default.Bookmark, stringResource(R.string.mylist_benefit_status_title), stringResource(R.string.mylist_benefit_status_subtitle))
                        HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = .5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        BenefitRow(Icons.Default.Star, stringResource(R.string.mylist_benefit_scores_title), stringResource(R.string.mylist_benefit_scores_subtitle))
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyListOptionsSheet(
    overlay: ListOverlay,
    uiState: MyListUiState,
    onSortSelected: (MyListSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    if (overlay == ListOverlay.INSIGHTS) {
        LibraryInsightsSheet(uiState, onDismiss)
        return
    }
    AppSheet(onDismissRequest = onDismiss, title = stringResource(R.string.mylist_sort)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 590.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MyListSortOrder.entries) { order ->
                AppChoiceRow(stringResource(order.labelRes), when (order) {
                    MyListSortOrder.RECENT -> Icons.Default.Update
                    MyListSortOrder.TITLE -> Icons.Default.SortByAlpha
                    MyListSortOrder.SCORE -> Icons.Default.Star
                    MyListSortOrder.PROGRESS -> Icons.Default.TrendingUp
                    MyListSortOrder.REMAINING -> Icons.Default.Timer
                    MyListSortOrder.WATCH_TIME -> Icons.Default.HourglassTop
                    MyListSortOrder.OLDEST -> Icons.Default.History
                    MyListSortOrder.LOWEST_SCORE -> Icons.Default.StarOutline
                }, order == uiState.sortOrder, { onSortSelected(order) })
            }
        }
    }
}
