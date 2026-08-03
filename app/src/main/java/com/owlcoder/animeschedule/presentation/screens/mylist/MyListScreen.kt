package com.owlcoder.animeschedule.presentation.screens.mylist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.AppErrorState
import com.owlcoder.animeschedule.presentation.components.AppLargeHeader
import com.owlcoder.animeschedule.presentation.components.AppLoadingState
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel

private val statusTabs = listOf(
    WatchStatus.WATCHING,
    WatchStatus.COMPLETED,
    WatchStatus.PLAN_TO_WATCH,
    WatchStatus.ON_HOLD,
    WatchStatus.DROPPED
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
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingAnimeId by remember { mutableStateOf<Int?>(null) }
    var showError by remember { mutableStateOf(false) }
    val editingEntry = uiState.entries.find { it.animeId == editingAnimeId }
    val context = LocalContext.current
    val toast = LocalToast.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)
    val totalCount = uiState.statusCounts.values.sum()

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                is MyListViewModel.UpdateEvent.Success -> {
                    showError = false
                    toast.success(savedMsg)
                }
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

    if (!uiState.isLoggedIn) {
        NotLoggedInState(onLogin = { authViewModel.launchMalLogin(context) })
        return
    }

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
            ) {
                AppLargeHeader(
                    title = stringResource(R.string.mylist_title),
                    subtitle = if (totalCount > 0) "$totalCount anime" else null,
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
                )
                MyListSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onClear = { viewModel.setSearchQuery("") }
                )
                StatusSegmentedControl(
                    activeFilter = uiState.activeFilter,
                    counts = uiState.statusCounts,
                    onFilterSelected = viewModel::setFilter,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = {
                showError = false
                viewModel.refresh()
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                uiState.isLoading && uiState.entries.isEmpty() -> AppLoadingState(
                    modifier = Modifier.fillMaxSize(),
                    label = stringResource(R.string.mylist_title)
                )
                showError && uiState.entries.isEmpty() -> AppErrorState(
                    title = errorMsg,
                    retryLabel = stringResource(R.string.common_retry),
                    onRetry = {
                        showError = false
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                uiState.entries.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    title = stringResource(R.string.mylist_empty_title),
                    subtitle = stringResource(R.string.mylist_empty_subtitle),
                    modifier = Modifier.fillMaxSize()
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = LocalNavBarHeight.current + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showError) {
                        item(key = "sync-error", contentType = "error") {
                            ErrorBanner(
                                message = errorMsg,
                                onRetry = {
                                    showError = false
                                    viewModel.refresh()
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    item(key = "list-group", contentType = "my-list-group") {
                        InsetGroup(
                            title = uiState.activeFilter.displayName(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Column {
                                uiState.entries.forEachIndexed { index, entry ->
                                    MyListEntryCard(
                                        entry = entry,
                                        title = entry.title.ifEmpty { entry.animeId.toString() },
                                        coverImageUrl = entry.coverImageUrl,
                                        isIncrementing = entry.animeId in uiState.pendingIncrementIds,
                                        onCardClick = { onAnimeClick(entry.animeId) },
                                        onIncrementEpisode = { viewModel.incrementEpisode(entry.animeId) },
                                        onEditStatus = { editingAnimeId = entry.animeId },
                                        showDivider = index < uiState.entries.lastIndex
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingAnimeId != null) {
        ListStatusBottomSheet(
            animeId = editingAnimeId!!,
            currentEntry = editingEntry,
            onDismiss = { editingAnimeId = null },
            onConfirm = { animeId, update -> viewModel.updateEntry(animeId, update) },
            onRemove = { animeId -> viewModel.removeEntry(animeId) }
        )
    }
}

@Composable
private fun MyListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        placeholder = { Text(stringResource(R.string.mylist_search_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear_recent))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun StatusSegmentedControl(
    activeFilter: WatchStatus,
    counts: Map<WatchStatus, Int>,
    onFilterSelected: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        statusTabs.forEach { status ->
            val selected = status == activeFilter
            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .clickable(onClick = { onFilterSelected(status) })
                    .semantics { role = Role.Tab },
                shape = RoundedCornerShape(9.dp),
                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                tonalElevation = if (selected) 1.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        status.tabIcon(selected),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = status.displayName(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    counts[status]?.takeIf { it > 0 }?.let {
                        Text(
                            it.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotLoggedInState(onLogin: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.mylist_not_logged_in_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.mylist_not_logged_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            GlassButton(onClick = onLogin) { contentColor ->
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.profile_login), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = contentColor)
            }
        }
    }
}
