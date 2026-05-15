package rs.owlcoder.animeschedule.presentation.screens.mylist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.owlcoder.animeschedule.domain.model.WatchStatus
import rs.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet

private val statusTabs = listOf(
    WatchStatus.WATCHING,
    WatchStatus.COMPLETED,
    WatchStatus.PLAN_TO_WATCH,
    WatchStatus.ON_HOLD,
    WatchStatus.DROPPED
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingAnimeId by remember { mutableStateOf<Int?>(null) }
    val editingEntry = uiState.entries.find { it.animeId == editingAnimeId }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Moja lista", style = MaterialTheme.typography.titleLarge) },
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
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = { Text("Pretraži moju listu") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Obriši")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = statusTabs.indexOf(uiState.activeFilter),
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    statusTabs.forEach { status ->
                        Tab(
                            selected = uiState.activeFilter == status,
                            onClick = { viewModel.setFilter(status) },
                            text = { Text(status.displayName) }
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
            if (uiState.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nema anime u kategoriji \"${uiState.activeFilter.displayName}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.entries, key = { it.animeId }) { entry ->
                        MyListEntryCard(
                            entry = entry,
                            title = entry.title.ifEmpty { entry.animeId.toString() },
                            coverImageUrl = entry.coverImageUrl,
                            onCardClick = { onAnimeClick(entry.animeId) },
                            onIncrementEpisode = { viewModel.incrementEpisode(entry.animeId) },
                            onEditStatus = { editingAnimeId = entry.animeId },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (editingAnimeId != null) {
        ListStatusBottomSheet(
            animeId = editingAnimeId!!,
            currentEntry = editingEntry,
            onDismiss = { editingAnimeId = null },
            onConfirm = { animeId, update -> viewModel.updateEntry(animeId, update) }
        )
    }
}
