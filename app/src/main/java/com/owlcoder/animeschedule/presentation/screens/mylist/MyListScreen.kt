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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchStatus
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
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

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
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingAnimeId by remember { mutableStateOf<Int?>(null) }
    var showError by remember { mutableStateOf(false) }
    val editingEntry = uiState.entries.find { it.animeId == editingAnimeId }
    val context = LocalContext.current
    val toast = LocalToast.current
    val motion = LocalMotionPolicy.current
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
                onEditStatus = { editingAnimeId = it },
            )
        }
    }

    editingAnimeId?.let { animeId ->
        ListStatusBottomSheet(
            animeId = animeId,
            currentEntry = editingEntry,
            onDismiss = { editingAnimeId = null },
            onConfirm = { id, update -> viewModel.updateEntry(id, update) },
            onRemove = { id -> viewModel.removeEntry(id) },
        )
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
    onFilterSelected: (WatchStatus) -> Unit,
    onAnimeClick: (Int) -> Unit,
    onIncrementEpisode: (Int) -> Unit,
    onEditStatus: (Int) -> Unit,
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
                    subtitle = if (totalCount > 0) "$totalCount anime" else null,
                    modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                )
                AppSearchField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.mylist_search_placeholder),
                    leadingIcon = Icons.Default.Search,
                    onClear = { onSearchQueryChange("") },
                    modifier = Modifier.height(44.dp),
                )
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                        title = stringResource(R.string.mylist_empty_title),
                        subtitle = stringResource(R.string.mylist_empty_subtitle),
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 5.dp, bottom = 116.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        item(key = "list-group", contentType = "my-list-group") {
                            InsetGroup(
                                title = uiState.activeFilter.displayName(),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Column {
                                    uiState.entries.forEachIndexed { index, entry ->
                                        MyListEntryCard(
                                            entry = entry,
                                            title = entry.title.ifEmpty { entry.animeId.toString() },
                                            coverImageUrl = entry.coverImageUrl,
                                            isIncrementing = entry.animeId in uiState.pendingIncrementIds,
                                            onCardClick = { onAnimeClick(entry.animeId) },
                                            onIncrementEpisode = { onIncrementEpisode(entry.animeId) },
                                            onEditStatus = { onEditStatus(entry.animeId) },
                                            showDivider = index < uiState.entries.lastIndex,
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
}

@Composable
private fun StatusFilterRow(
    activeFilter: WatchStatus,
    counts: Map<WatchStatus, Int>,
    onFilterSelected: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionPolicy.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        statusTabs.forEach { status ->
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
                    .clickable(onClick = { onFilterSelected(status) })
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.height(34.dp),
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
                                status.tabIcon(selectedState),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor,
                            )
                        }
                        Text(
                            text = status.displayName(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor,
                        )
                        counts[status]?.takeIf { it > 0 }?.let { count ->
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
