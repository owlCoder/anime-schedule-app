package com.owlcoder.animeschedule.presentation.screens.search

import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.domain.model.MalListUpdate
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.ui.theme.GlassTone

/**
 * Search as a normal destination. It intentionally owns no page-sized card: the page
 * background remains visible and only the input uses the glass treatment.
 */
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
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)
    val currentFocusCallback by rememberUpdatedState(onFocusChanged)

    SearchStatusBarStyle()
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
        if (!isFocused) {
            AppLargeHeader(
                title = stringResource(R.string.search_title),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
            if (isFocused) {
                TextButton(
                    onClick = {
                        clearFocusAndKeyboard()
                        onCancel()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SearchContent(
            query = query,
            recentSearches = recentSearches,
            uiState = uiState,
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

/** Compatibility bridge for the old shell. New navigation should use [SearchScreen]. */
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
    GlassSurface(
        modifier = modifier
            .height(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onFieldTap,
            ),
        shape = RoundedCornerShape(14.dp),
        tone = GlassTone.Neutral,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
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
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
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
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onClear)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_clear_recent),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Search owns a black/white page background, so its status icons must follow that page. */
@Composable
private fun SearchStatusBarStyle() {
    val view = LocalView.current
    val useDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(view, useDarkIcons) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = useDarkIcons
        onDispose {
            if (previous != null) controller.isAppearanceLightStatusBars = previous
        }
    }
}

@Composable
private fun SearchContent(
    query: String,
    recentSearches: List<String>,
    uiState: SearchUiState,
    onClearRecent: () -> Unit,
    onRecentClick: (String) -> Unit,
    onAnimeClick: (AnimeSearchResult) -> Unit,
    onEditStatus: (AnimeSearchResult) -> Unit,
    onRetry: () -> Unit,
    onClearQuery: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when {
        query.isBlank() && recentSearches.isNotEmpty() -> RecentSearches(
            searches = recentSearches,
            onClear = onClearRecent,
            onSelect = onRecentClick,
        )
        query.isBlank() -> CompactSearchState(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_empty_title),
            subtitle = stringResource(R.string.search_empty_subtitle),
        )
        uiState.isLoading -> SearchLoadingState()
        uiState.errorRes != null -> CompactSearchState(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.search_title),
            subtitle = stringResource(uiState.errorRes),
            actionLabel = stringResource(R.string.common_retry),
            onAction = onRetry,
        )
        uiState.noResults -> CompactSearchState(
            icon = Icons.Default.Search,
            title = stringResource(R.string.search_no_results_title),
            subtitle = stringResource(R.string.search_no_results_subtitle),
            actionLabel = stringResource(R.string.search_clear_recent),
            onAction = onClearQuery,
        )
        else -> SearchResults(
            results = uiState.results,
            hasNextPage = uiState.hasNextPage,
            isLoadingMore = uiState.isLoadingMore,
            onAnimeClick = onAnimeClick,
            onEditStatus = onEditStatus,
            onLoadMore = onLoadMore,
        )
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onClear: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.search_recent),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text(stringResource(R.string.search_clear_recent)) }
        }
        InsetGroup {
            searches.forEachIndexed { index, recent ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = { onSelect(recent) })
                        .semantics { role = Role.Button }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = recent,
                        style = MaterialTheme.typography.bodyLarge,
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
    onAnimeClick: (AnimeSearchResult) -> Unit,
    onEditStatus: (AnimeSearchResult) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "results_header") {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        item(key = "results_group") {
            InsetGroup {
                results.forEach { result ->
                    SearchResultCard(
                        result = result,
                        onCardClick = { onAnimeClick(result) },
                        onEditStatus = if (result.malId != null) ({ onEditStatus(result) }) else null,
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
            }
        }
    }
}

@Composable
private fun SearchLoadingState() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.search_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        InsetGroup {
            repeat(4) { index ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(88.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(52.dp, 70.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.fillMaxWidth(0.62f).height(13.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                        Box(Modifier.fillMaxWidth(0.38f).height(11.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                    }
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
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
