package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.owlcoder.animeschedule.presentation.components.iosSpring
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
        transitionSpec = {
            (fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                scaleIn(initialScale = 0.985f, animationSpec = motion.iosTween(IosMotion.Standard))) togetherWith
                (fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                    scaleOut(targetScale = 0.995f, animationSpec = motion.iosTween(IosMotion.Quick)))
        },
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
                onClearQuickFilters = viewModel::clearQuickFilters,
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
        ListOverlay.VIEWS -> SavedViewsSheet(uiState.tools.savedViews, viewModel::saveView, { viewModel.applyView(it); overlay = null }, viewModel::deleteView, { overlay = null })
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
private fun LoggedInList(
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
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .animateContentSize(animationSpec = motion.iosSpring()),
            ) {
                AppLargeHeader(
                    title = stringResource(R.string.mylist_title),
                    subtitle = totalCount.takeIf { it > 0 }?.let { count ->
                        stringResource(R.string.mylist_anime_count, count)
                    },
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    trailingContent = {
                        GlassToolbarGroup {
                            GlassToolbarButton(Icons.Default.BarChart, stringResource(R.string.mylist_statistics), onShowInsights)
                            GlassToolbarButton(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.mylist_sort), onShowSort)
                            GlassToolbarButton(Icons.Default.MoreHoriz, stringResource(R.string.list_tools), onShowTools)
                        }
                    },
                )
                InsightsStrip(uiState.insights, onShowInsights)
                AppSearchField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.mylist_search_placeholder),
                    leadingIcon = Icons.Default.Search,
                    onClear = { onSearchQueryChange("") },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(uiState.smartFilter != SmartListFilter.ALL, onSmart, modifier = Modifier.testTag("list-smart-filter"), label = { Text(stringResource(if (uiState.smartFilter == SmartListFilter.ALL) R.string.smart_filters else uiState.smartFilter.labelRes())) }, leadingIcon = { Icon(Icons.Default.FilterAlt, null, Modifier.size(16.dp)) })
                    FilterChip(uiState.scoreRange != (0 to 10), onRating, modifier = Modifier.testTag("list-rating-filter"), label = { Text(if (uiState.scoreRange == (0 to 10)) stringResource(R.string.rating_filter) else stringResource(R.string.rating_value, uiState.scoreRange.first, uiState.scoreRange.second)) }, leadingIcon = { Icon(Icons.Default.StarHalf, null, Modifier.size(16.dp)) })
                    FilterChip(uiState.favoritesOnly, onFavorites, modifier = Modifier.testTag("list-favorites-filter"), label = { Text(stringResource(R.string.favorites)) }, leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(16.dp)) })
                    FilterChip(uiState.unratedOnly, onUnrated, modifier = Modifier.testTag("list-unrated-filter"), label = { Text(stringResource(R.string.list_unrated)) }, leadingIcon = { Icon(Icons.Default.StarBorder, null, Modifier.size(16.dp)) })
                    FilterChip(uiState.activeTag != null, onTags, modifier = Modifier.testTag("list-tags-filter"), label = { Text(uiState.activeTag ?: stringResource(R.string.personal_tags)) }, leadingIcon = { Icon(Icons.Default.Label, null, Modifier.size(16.dp)) })
                    FilterChip(false, onViews, modifier = Modifier.testTag("list-saved-views"), label = { Text(stringResource(R.string.saved_list_views)) }, leadingIcon = { Icon(Icons.Default.Bookmarks, null, Modifier.size(16.dp)) })
                }
                val activeCount = listOf(uiState.searchQuery.isNotBlank(), uiState.favoritesOnly, uiState.unratedOnly, uiState.activeTag != null, uiState.smartFilter != SmartListFilter.ALL, uiState.scoreRange != (0 to 10)).count { it }
                if (activeCount > 0) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.active_filter_count, activeCount) + " · " + stringResource(R.string.visible_results, uiState.entries.size), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClearQuickFilters, Modifier.testTag("list-clear-active")) { Icon(Icons.Default.FilterAltOff, stringResource(R.string.list_clear_filters), Modifier.size(20.dp)) }
                }
                StatusFilterRow(
                    activeFilter = uiState.activeFilter,
                    counts = uiState.statusCounts,
                    onFilterSelected = onFilterSelected,
                    modifier = Modifier.padding(top = 7.dp, bottom = 6.dp),
                )
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
            val contentMode = when {
                uiState.isLoading && uiState.entries.isEmpty() -> 0
                showError && uiState.entries.isEmpty() -> 1
                uiState.entries.isEmpty() -> 2
                else -> 3
            }
            AnimatedContent(
                targetState = contentMode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) togetherWith
                        fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                },
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
                        title = stringResource(if (uiState.searchQuery.isNotBlank() || uiState.favoritesOnly || uiState.unratedOnly || uiState.activeTag != null || uiState.smartFilter != SmartListFilter.ALL || uiState.scoreRange != (0 to 10)) R.string.list_filtered_empty else R.string.mylist_empty_title),
                        subtitle = stringResource(if (uiState.searchQuery.isNotBlank() || uiState.favoritesOnly || uiState.unratedOnly || uiState.activeTag != null || uiState.smartFilter != SmartListFilter.ALL || uiState.scoreRange != (0 to 10)) R.string.list_filtered_empty_hint else R.string.mylist_empty_subtitle),
                        actionLabel = if (uiState.searchQuery.isNotBlank() || uiState.favoritesOnly || uiState.unratedOnly || uiState.activeTag != null || uiState.smartFilter != SmartListFilter.ALL || uiState.scoreRange != (0 to 10)) stringResource(R.string.list_clear_filters) else null,
                        onAction = if (uiState.searchQuery.isNotBlank() || uiState.favoritesOnly || uiState.unratedOnly || uiState.activeTag != null || uiState.smartFilter != SmartListFilter.ALL || uiState.scoreRange != (0 to 10)) onClearQuickFilters else null,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 5.dp, bottom = 116.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        item(key = "list-section-title", contentType = "section-title") {
                            Text(
                                text = "${uiState.activeFilter?.displayName() ?: stringResource(R.string.list_all_statuses)} · ${stringResource(uiState.sortOrder.labelRes)}",
                                modifier = Modifier.padding(
                                    start = 27.dp,
                                    end = 27.dp,
                                    top = 2.dp,
                                    bottom = 1.dp,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
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
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
                animationSpec = motion.iosTween(IosMotion.Standard),
                label = "my-list-tab-fill",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.97f,
                animationSpec = motion.iosSpring(),
                label = "my-list-tab-scale",
            )
            Box(
                modifier = Modifier
                    .sizeIn(minHeight = 44.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag("list-status-${status?.name ?: "ALL"}")
                    .clickable(onClick = { onFilterSelected(status) })
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.heightIn(min = 38.dp),
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
                        modifier = Modifier.padding(horizontal = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) togetherWith
                                    fadeOut(animationSpec = motion.iosTween(IosMotion.Quick))
                            },
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
private fun NotLoggedInState(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        AppLargeHeader(
            title = stringResource(R.string.mylist_title),
            modifier = Modifier.padding(top = 6.dp),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(start = 4.dp, end = 4.dp, bottom = 86.dp),
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.mylist_not_logged_in_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.mylist_not_logged_in_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AppMaterialSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    material = AppMaterial.Grouped,
                    shape = ContinuousRoundedShape(20.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BenefitRow(
                            icon = Icons.Default.CloudSync,
                            title = stringResource(R.string.mylist_benefit_sync_title),
                            subtitle = stringResource(R.string.mylist_benefit_sync_subtitle),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        BenefitRow(
                            icon = Icons.Default.Bookmark,
                            title = stringResource(R.string.mylist_benefit_status_title),
                            subtitle = stringResource(R.string.mylist_benefit_status_subtitle),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        BenefitRow(
                            icon = Icons.Default.Star,
                            title = stringResource(R.string.mylist_benefit_scores_title),
                            subtitle = stringResource(R.string.mylist_benefit_scores_subtitle),
                        )
                    }
                }
                AppButton(
                    label = stringResource(R.string.profile_login),
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 320.dp)
                        .padding(top = 10.dp),
                    variant = AppButtonVariant.Primary,
                    icon = Icons.AutoMirrored.Filled.Login,
                )
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


@Composable
private fun InsightsStrip(insights: MyListInsights, onClick: (() -> Unit)? = null) {
    AppMaterialSurface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        material = AppMaterial.Interactive,
        shape = ContinuousRoundedShape(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InsightMetric(insights.completedAnime.toString(), stringResource(R.string.mylist_metric_completed), Modifier.weight(1f))
            InsightMetric(insights.watchedEpisodes.toString(), stringResource(R.string.mylist_metric_episodes), Modifier.weight(1f))
            InsightMetric(insights.averageScore?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "—", stringResource(R.string.mylist_metric_score), Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsightMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    AppSheet(onDismissRequest = onDismiss, title = stringResource(if (overlay == ListOverlay.SORT) R.string.mylist_sort else R.string.mylist_statistics)) {
        if (overlay == ListOverlay.SORT) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MyListSortOrder.entries.forEach { order ->
                AppChoiceRow(stringResource(order.labelRes), when (order) {
                    MyListSortOrder.RECENT -> Icons.Default.Update
                    MyListSortOrder.TITLE -> Icons.Default.SortByAlpha
                    MyListSortOrder.SCORE -> Icons.Default.Star
                    MyListSortOrder.PROGRESS -> Icons.Default.TrendingUp
                    MyListSortOrder.REMAINING -> Icons.Default.Timer
                    MyListSortOrder.WATCH_TIME -> Icons.Default.HourglassTop
                }, order == uiState.sortOrder, { onSortSelected(order) })
            }
            }
        } else {
            InsightsStrip(uiState.insights)
            Text(
                text = stringResource(R.string.mylist_backlog, uiState.insights.remainingEpisodes),
                modifier = Modifier.padding(vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.mylist_backlog_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            statusTabs.forEach { status ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(status.tabIcon(false), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(status.displayName(), modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text((uiState.statusCounts[status] ?: 0).toString(), fontWeight = FontWeight.SemiBold)
                }
            }
            Text(stringResource(R.string.mylist_rated_count, uiState.insights.ratedAnime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
