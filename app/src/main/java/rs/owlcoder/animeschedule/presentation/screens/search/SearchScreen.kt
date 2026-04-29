package rs.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.owlcoder.animeschedule.presentation.components.ErrorBanner
import rs.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    var editingResult by remember { mutableStateOf<rs.owlcoder.animeschedule.domain.model.AnimeSearchResult?>(null) }
    var showRecent by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Pretraga", style = MaterialTheme.typography.titleLarge) },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TextField(
                    value = uiState.query,
                    onValueChange = { viewModel.setQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .onFocusChanged { showRecent = it.isFocused },
                    placeholder = { Text("Pretraži anime bazu MAL") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Obriši",
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.onSearchSubmit(uiState.query)
                        showRecent = false
                        focusManager.clearFocus()
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when {
                showRecent && recentSearches.isNotEmpty() -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Nedavne pretrage",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearRecentSearches() }) { Text("Obriši") }
                            }
                        }
                        items(recentSearches) { recent ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setQuery(recent)
                                        viewModel.onSearchSubmit(recent)
                                        showRecent = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(recent, modifier = Modifier.padding(start = 12.dp).weight(1f))
                            }
                        }
                    }
                }
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.error != null -> ErrorBanner(uiState.error!!)
                uiState.noResults -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nema rezultata za \"${uiState.query}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                uiState.query.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pretraži po naslovu anime", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.results, key = { it.malId }) { result ->
                        SearchResultCard(
                            result = result,
                            onCardClick = { onAnimeClick(result.malId); viewModel.onSearchSubmit(uiState.query) },
                            onEditStatus = { editingResult = result },
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

    editingResult?.let { result ->
        ListStatusBottomSheet(
            animeId = result.malId,
            currentEntry = result.userListEntry,
            onDismiss = { editingResult = null },
            onConfirm = { animeId, update -> viewModel.updateListEntry(animeId, update) }
        )
    }
}
