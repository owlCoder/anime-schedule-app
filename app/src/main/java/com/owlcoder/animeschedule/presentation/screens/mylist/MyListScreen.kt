package com.owlcoder.animeschedule.presentation.screens.mylist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: MyListViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingAnimeId by remember { mutableStateOf<Int?>(null) }
    val editingEntry = uiState.entries.find { it.animeId == editingAnimeId }
    val context = androidx.compose.ui.platform.LocalContext.current
    val toast = LocalToast.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                is MyListViewModel.UpdateEvent.Success -> toast.success(savedMsg)
                is MyListViewModel.UpdateEvent.Removed -> toast.success(removedMsg)
                is MyListViewModel.UpdateEvent.Error -> toast.error(errorMsg)
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
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.mylist_title), style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(MaterialTheme.shapes.medium),
                    placeholder = { Text(stringResource(R.string.mylist_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear_recent))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statusTabs.forEach { status ->
                        val isSelected = uiState.activeFilter == status
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                          else Color.Transparent,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chip_bg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                          else MaterialTheme.colorScheme.outlineVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chip_border"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "chip_text"
                        )
                        val count = uiState.statusCounts[status] ?: 0
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(bgColor)
                                .border(BorderStroke(1.dp, borderColor), PillShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.setFilter(status) }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                status.tabIcon(isSelected),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = textColor
                            )
                            Text(
                                text = status.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = textColor
                            )
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
            if (uiState.entries.isEmpty() && !uiState.isLoading) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    title = stringResource(R.string.mylist_empty_title),
                    subtitle = stringResource(R.string.mylist_empty_subtitle)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.entries, key = { it.animeId }) { entry ->
                        MyListEntryCard(
                            entry = entry,
                            title = entry.title.ifEmpty { entry.animeId.toString() },
                            coverImageUrl = entry.coverImageUrl,
                            isIncrementing = entry.animeId in uiState.pendingIncrementIds,
                            onCardClick = { onAnimeClick(entry.animeId) },
                            onIncrementEpisode = { viewModel.incrementEpisode(entry.animeId) },
                            onEditStatus = { editingAnimeId = entry.animeId },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    item { val h = LocalNavBarHeight.current; Spacer(Modifier.height(h + 8.dp)) }
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
private fun NotLoggedInState(onLogin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.mylist_not_logged_in_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.mylist_not_logged_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            GlassButton(onClick = { onLogin() }) { contentColor ->
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    stringResource(R.string.profile_login),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}
