package com.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
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
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween

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
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
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
                modifier = Modifier.weight(1f),
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
        SearchContent(
            query = query,
            recentSearches = recentSearches,
            uiState = uiState,
            bottomPadding = if (isFocused) 24.dp else 112.dp,
            onClearRecent = viewModel::clearRecentSearches,
            onRecentClick = { recent ->
                query = recent
                viewModel.setQuery(recent)
                viewModel.onSearchSubmit(recent)
                requestInputFocus()
            },
            onAnimeClick = { result ->
                viewModel.onSearchSubmit(query)
                clearFocusAndKeyboard()
                onAnimeClick(result.anilistId)
            },
            onEditStatus = { editingResult = it },
            onRetry = viewModel::retrySearch,
            onClearQuery = {
                query = ""
                viewModel.setQuery("")
            },
            onLoadMore = viewModel::loadMore,
        )
    }

    editingResult?.let { result ->
        result.malId?.let { malId ->
            val liveEntry = uiState.results.find { it.anilistId == result.anilistId }?.userListEntry
                ?: result.userListEntry
            ListStatusBottomSheet(
                animeId = malId,
                currentEntry = liveEntry,
                onDismiss = { editingResult = null },
                onConfirm = { animeId, update: MalListUpdate -> viewModel.updateListEntry(animeId, update) },
                onRemove = { animeId -> viewModel.removeListEntry(animeId) },
            )
        }
    }
}

@Deprecated("Use SearchScreen as a navigation destination")
@Composable
fun SearchOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAnimeClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    onFocusChanged: (Boolean) -> Unit = {},
) {
    if (visible) {
        SearchScreen(
            onAnimeClick = onAnimeClick,
            viewModel = viewModel,
            onFocusChanged = onFocusChanged,
            onCancel = onDismiss,
            requestFocus = true,
        )
    } else {
        LaunchedEffect(Unit) { onFocusChanged(false) }
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
    AppMaterialSurface(
        modifier = modifier
            .height(48.dp)
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
                        text = stringResource(R.string.search_placeholder),
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
        query.isBlank() -> SearchContentMode.Empty
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
            )
            SearchContentMode.Empty -> CompactSearchState(
                icon = Icons.Default.Search,
                title = stringResource(R.string.search_empty_title),
                subtitle = stringResource(R.string.search_empty_subtitle),
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
                actionLabel = stringResource(R.string.search_clear_recent),
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
            )
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onClear: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_recent),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text(
                    stringResource(R.string.search_clear_recent),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        InsetGroup {
            searches.forEachIndexed { index, recent ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(onClick = { onSelect(recent) })
                        .semantics { role = Role.Button }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = recent,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<AnimeSearchResult>,
    hasNextPage: Boolean,
    isLoadingMore: Boolean,
    bottomPadding: Dp,
    onAnimeClick: (AnimeSearchResult) -> Unit,
    onEditStatus: (AnimeSearchResult) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),
    ) {
        item(key = "results_group") {
            InsetGroup {
                results.forEachIndexed { index, result ->
                    SearchResultCard(
                        result = result,
                        onCardClick = { onAnimeClick(result) },
                        onEditStatus = if (result.malId != null) ({ onEditStatus(result) }) else null,
                        showDivider = index < results.lastIndex,
                    )
                }
            }
        }
        if (hasNextPage || isLoadingMore) {
            item(key = "load_more") {
                LaunchedEffect(results.size, isLoadingMore) {
                    if (hasNextPage && !isLoadingMore) onLoadMore()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun SearchLoadingState() {
    InsetGroup {
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
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(9.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.58f)
                            .height(11.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.34f)
                            .height(9.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
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
