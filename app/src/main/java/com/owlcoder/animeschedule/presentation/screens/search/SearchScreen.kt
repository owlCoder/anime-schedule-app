package com.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Tune
import com.owlcoder.animeschedule.presentation.components.AppLoadingState
import com.owlcoder.animeschedule.presentation.components.rememberSkeletonShimmer
import androidx.compose.foundation.progressSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalToast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class SearchContentMode { Recents, Empty, Loading, Error, NoResults, Results }

@Composable
fun SearchScreen(
    onAnimeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onFocusChanged: (Boolean) -> Unit = {},
    onCancel: () -> Unit = {},
    requestFocus: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var editingResult by remember { mutableStateOf<AnimeSearchResult?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val toast = LocalToast.current
    val motion = LocalMotionPolicy.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)
    val currentFocusCallback by rememberUpdatedState(onFocusChanged)

    DisposableEffect(Unit) {
        onDispose { currentFocusCallback(false) }
    }
    // After process death the field text is restored but the ViewModel starts empty, so push the
    // restored text back; for a surviving ViewModel this is a no-op (same value).
    LaunchedEffect(Unit) {
        if (query.isNotBlank()) viewModel.setQuery(query)
    }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            isFocused = true
            currentFocusCallback(true)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                SearchViewModel.UpdateEvent.Success -> toast.success(savedMsg)
                SearchViewModel.UpdateEvent.Removed -> toast.success(removedMsg)
                SearchViewModel.UpdateEvent.Error -> toast.error(errorMsg)
            }
        }
    }

    fun clearFocusAndKeyboard() {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    fun updateFocus(focused: Boolean) {
        if (isFocused != focused) {
            isFocused = focused
            currentFocusCallback(focused)
        }
        if (focused) keyboard?.show()
    }

    fun requestInputFocus() {
        updateFocus(true)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        AnimatedVisibility(
            visible = !isFocused,
            enter = slideInVertically(
                animationSpec = motion.iosTween(IosMotion.Standard),
                initialOffsetY = { -it / 4 },
            ) + fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)),
            exit = slideOutVertically(
                animationSpec = motion.iosTween(IosMotion.Quick),
                targetOffsetY = { -it / 4 },
            ) + fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)),
        ) {
            AppLargeHeader(
                title = stringResource(R.string.search_title),
                modifier = Modifier.padding(top = 6.dp, bottom = 5.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = motion.iosSpring()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)) +
                    scaleIn(initialScale = 0.88f, animationSpec = motion.iosSpring()),
                exit = fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                    scaleOut(targetScale = 0.9f, animationSpec = motion.iosTween(IosMotion.Quick)),
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = {
                        clearFocusAndKeyboard()
                        onCancel()
                    },
                )
            }
            SearchField(
                query = query,
                modifier = Modifier.weight(1f).testTag("anime-search-field"),
                focusRequester = focusRequester,
                onFieldTap = ::requestInputFocus,
                onFocusChanged = ::updateFocus,
                onQueryChange = {
                    query = it
                    viewModel.setQuery(it)
                },
                onSubmit = {
                    viewModel.onSearchSubmit(query)
                    clearFocusAndKeyboard()
                },
                onClear = {
                    query = ""
                    viewModel.setQuery("")
                },
            )
        }

        Spacer(Modifier.height(if (isFocused) 10.dp else 12.dp))
        if (query.trim().length >= 2) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                if (maxWidth < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppButton(stringResource(R.string.discovery_search_filters), { clearFocusAndKeyboard(); showFilters = true }, variant = AppButtonVariant.Secondary, icon = Icons.Default.Tune, modifier = Modifier.fillMaxWidth().testTag("search-filters"))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.discovery_result_count, uiState.results.size, uiState.loadedCount), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (uiState.filter.isActive) GlassIconButton(Icons.Default.RestartAlt, stringResource(R.string.seasonal_filter_reset), viewModel::clearFilter)
                        }
                    }
                } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(stringResource(R.string.discovery_search_filters), { clearFocusAndKeyboard(); showFilters = true }, variant = AppButtonVariant.Secondary, icon = Icons.Default.Tune, modifier = Modifier.testTag("search-filters"))
                    Text(stringResource(R.string.discovery_result_count, uiState.results.size, uiState.loadedCount), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.filter.isActive) GlassIconButton(Icons.Default.RestartAlt, stringResource(R.string.seasonal_filter_reset), viewModel::clearFilter)
                }
            }
        }
        SearchContent(
            query = query,
            recentSearches = recentSearches,
            uiState = uiState,
            bottomPadding = if (isFocused) 24.dp else 112.dp,
            onClearRecent = viewModel::clearRecentSearches,
            onRemoveRecent = viewModel::removeRecentSearch,
            onClearFilter = viewModel::clearFilter,
            onRecentClick = { recent ->
                query = recent
                viewModel.setQuery(recent)
                viewModel.onSearchSubmit(recent)
                clearFocusAndKeyboard()
            },
            onAnimeClick = { result ->
                viewModel.onSearchSubmit(query)
                clearFocusAndKeyboard()
                onAnimeClick(result.anilistId)
            },
            onEditStatus = { clearFocusAndKeyboard(); editingResult = it },
            onRetry = viewModel::retrySearch,
            onClearQuery = {
                query = ""
                viewModel.setQuery("")
            },
            onLoadMore = viewModel::loadMore,
        )
    }

    if (showFilters) SearchFilterSheet(uiState.filter, uiState.availableFormats, viewModel::setTracking, viewModel::toggleFormat, viewModel::setSort, viewModel::clearFilter, { showFilters = false },
        uiState.availableYears, viewModel::setMinimumScore, viewModel::setLength, viewModel::setYear, viewModel::setMaximumScore, viewModel::setWatchStatus)

    editingResult?.let { result ->
        result.malId?.let { malId ->
            val liveEntry = uiState.results.find { it.anilistId == result.anilistId }?.userListEntry
                ?: result.userListEntry
            ListStatusBottomSheet(
                animeId = malId,
                currentEntry = liveEntry,
                animeTitle = result.title,
                totalEpisodes = result.totalEpisodes ?: liveEntry?.totalEpisodes,
                onDismiss = { editingResult = null },
                onConfirm = { animeId, update: MalListUpdate -> viewModel.updateListEntry(animeId, update) },
                onRemove = { animeId -> viewModel.removeListEntry(animeId) },
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    modifier: Modifier,
    focusRequester: FocusRequester,
    onFieldTap: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val motion = LocalMotionPolicy.current
    val placeholder = stringResource(R.string.search_placeholder)
    AppMaterialSurface(
        modifier = modifier
            .height(52.dp)
            .animateContentSize(animationSpec = motion.iosSpring())
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onFieldTap,
            ),
        material = AppMaterial.Elevated,
        shape = ContinuousRoundedShape(17.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 13.dp, end = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 9.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = placeholder }
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = motion.iosSpring(),
                ) + fadeIn(animationSpec = motion.iosTween(IosMotion.Quick)),
                exit = scaleOut(
                    targetScale = 0.85f,
                    animationSpec = motion.iosTween(IosMotion.Quick),
                ) + fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            role = Role.Button,
                            onClick = onClear,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        tonalElevation = 0.dp,
                    ) {
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.search_clear_recent),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContent(
    query: String,
    recentSearches: List<String>,
    uiState: SearchUiState,
    bottomPadding: Dp,
    onClearRecent: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearFilter: () -> Unit,
    onRecentClick: (String) -> Unit,
    onAnimeClick: (AnimeSearchResult) -> Unit,
    onEditStatus: (AnimeSearchResult) -> Unit,
    onRetry: () -> Unit,
    onClearQuery: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val motion = LocalMotionPolicy.current
    val mode = when {
        query.isBlank() && recentSearches.isNotEmpty() -> SearchContentMode.Recents
        query.trim().length < 2 -> SearchContentMode.Empty
        uiState.isLoading -> SearchContentMode.Loading
        uiState.errorRes != null -> SearchContentMode.Error
        uiState.noResults -> SearchContentMode.NoResults
        else -> SearchContentMode.Results
    }

    AnimatedContent(
        targetState = mode,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                scaleIn(initialScale = 0.99f, animationSpec = motion.iosTween(IosMotion.Standard))) togetherWith
                (fadeOut(animationSpec = motion.iosTween(IosMotion.Quick)) +
                    scaleOut(targetScale = 0.995f, animationSpec = motion.iosTween(IosMotion.Quick)))
        },
        label = "search-content",
    ) { contentMode ->
        when (contentMode) {
            SearchContentMode.Recents -> RecentSearches(
                searches = recentSearches,
                onClear = onClearRecent,
                onSelect = onRecentClick,
                onRemove = onRemoveRecent,
            )
            SearchContentMode.Empty -> CompactSearchState(
                icon = Icons.Default.Search,
                title = stringResource(R.string.search_empty_title),
                subtitle = stringResource(if (query.isBlank()) R.string.search_empty_subtitle else R.string.discovery_min_query),
            )
            SearchContentMode.Loading -> SearchLoadingState()
            SearchContentMode.Error -> CompactSearchState(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.search_title),
                subtitle = uiState.errorRes?.let { stringResource(it) }.orEmpty(),
                actionLabel = stringResource(R.string.common_retry),
                onAction = onRetry,
            )
            SearchContentMode.NoResults -> CompactSearchState(
                icon = Icons.Default.Search,
                title = stringResource(R.string.search_no_results_title),
                subtitle = stringResource(R.string.search_no_results_subtitle),
                actionLabel = stringResource(R.string.search_clear_query),
                onAction = onClearQuery,
            )
            SearchContentMode.Results -> SearchResults(
                results = uiState.results,
                hasNextPage = uiState.hasNextPage,
                isLoadingMore = uiState.isLoadingMore,
                bottomPadding = bottomPadding,
                onAnimeClick = onAnimeClick,
                onEditStatus = onEditStatus,
                onLoadMore = onLoadMore,
                loadMoreError = uiState.loadMoreError,
                onClearFilter = onClearFilter,
            )
        }
    }
}

@Composable
internal fun RecentSearches(searches: List<String>, onClear: () -> Unit, onSelect: (String) -> Unit, onRemove: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 112.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.search_recent), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AppButton(stringResource(R.string.search_clear_recent), onClear, variant = AppButtonVariant.Plain, icon = Icons.Default.DeleteSweep)
            }
        }
        items(searches, key = { it }) { recent ->
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.weight(1f).clickable(role = Role.Button) { onSelect(recent) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.History, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(recent, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    GlassIconButton(Icons.Default.Close, stringResource(R.string.discovery_remove_recent, recent), { onRemove(recent) }, Modifier.testTag("recent-remove-$recent"))
                }
            }
        }
    }
}

@Composable
internal fun SearchResults(results: List<AnimeSearchResult>, hasNextPage: Boolean, isLoadingMore: Boolean, bottomPadding: Dp, onAnimeClick: (AnimeSearchResult) -> Unit, onEditStatus: (AnimeSearchResult) -> Unit, onLoadMore: () -> Unit, loadMoreError: Boolean = false, onClearFilter: () -> Unit = {}) {
    LazyColumn(Modifier.fillMaxSize().testTag("search-results"), contentPadding = PaddingValues(bottom = bottomPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (results.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.discovery_filtered_empty), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.discovery_filtered_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AppButton(stringResource(R.string.seasonal_filter_reset), onClearFilter, icon = Icons.Default.RestartAlt, variant = AppButtonVariant.Secondary)
            }
        }
        items(results, key = { it.anilistId }, contentType = { "search-result" }) { result ->
            SearchResultCard(result, { onAnimeClick(result) }, if (result.malId != null) ({ onEditStatus(result) }) else null, showDivider = false)
        }
        if (hasNextPage || isLoadingMore) item(key = "load_more") {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (loadMoreError) Text(stringResource(R.string.discovery_page_error), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                if (isLoadingMore) AppLoadingState(Modifier.fillMaxWidth())
                else AppButton(stringResource(if (loadMoreError) R.string.common_retry else R.string.discovery_load_more), onLoadMore, Modifier.fillMaxWidth().testTag("search-load-more"), icon = Icons.Default.Search, variant = AppButtonVariant.Secondary)
            }
        }
    }
}

@Composable
internal fun SearchLoadingState() {
    val shimmer = rememberSkeletonShimmer()
    InsetGroup(Modifier.progressSemantics().testTag("search-loading")) {
        repeat(4) { index ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(40.dp, 54.dp)
                        .clip(RoundedCornerShape(9.dp)).then(shimmer),
                )
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.58f)
                            .height(11.dp)
                            .clip(CircleShape).then(shimmer),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.34f)
                            .height(9.dp)
                            .clip(CircleShape).then(shimmer),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSearchState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    EmptyState(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.fillMaxSize(),
        actionLabel = actionLabel,
        onAction = onAction,
    )
}
