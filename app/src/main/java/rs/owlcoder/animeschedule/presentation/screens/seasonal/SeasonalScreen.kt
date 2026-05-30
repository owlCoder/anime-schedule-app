package rs.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import rs.owlcoder.animeschedule.domain.model.AnimeSeason
import rs.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import rs.owlcoder.animeschedule.presentation.components.EmptyState
import rs.owlcoder.animeschedule.presentation.components.LoadingShimmer

private val FORMAT_LABELS = mapOf(
    "TV" to "TV", "TV_SHORT" to "TV Short", "MOVIE" to "Film",
    "SPECIAL" to "Specijal", "OVA" to "OVA", "ONA" to "ONA", "MUSIC" to "Music"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalScreen(
    onAnimeClick: (Int) -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            "${uiState.season.displayName} ${uiState.year}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            val (prev, prevYear) = prevSeason(uiState.season, uiState.year)
                            viewModel.setSeason(prev, prevYear)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Prethodna sezona")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val (next, nextYear) = nextSeason(uiState.season, uiState.year)
                            viewModel.setSeason(next, nextYear)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Sledeća sezona")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(badge = { if (uiState.filter.isActive) Badge() }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filteri",
                                    tint = if (uiState.filter.isActive)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
                SeasonTabRow(
                    currentSeason = uiState.season,
                    currentYear = uiState.year,
                    onSelect = { s, y -> viewModel.setSeason(s, y) }
                )
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.error != null -> EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "Greška",
                    subtitle = uiState.error ?: ""
                )
                uiState.filteredItems.isEmpty() -> EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = "Nema rezultata",
                    subtitle = "Pokušaj drugačije filtere"
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.filteredItems, key = { it.anilistId }) { item ->
                        SeasonalAnimeCard(
                            item = item,
                            onClick = { onAnimeClick(item.anilistId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }

    if (showFilterSheet) {
        SeasonalFilterSheet(
            filter = uiState.filter,
            availableGenres = uiState.availableGenres,
            availableFormats = uiState.availableFormats,
            onGenreToggle = { viewModel.toggleGenre(it) },
            onFormatToggle = { viewModel.toggleFormat(it) },
            onSortChange = { viewModel.setSortOrder(it) },
            onClear = { viewModel.clearFilter() },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun SeasonTabRow(
    currentSeason: AnimeSeason,
    currentYear: Int,
    onSelect: (AnimeSeason, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimeSeason.entries.forEach { s ->
            val isSelected = s == currentSeason
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "season_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "season_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(s, currentYear) }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun SeasonalAnimeCard(
    item: SeasonalAnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.coverImageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                val meta = listOfNotNull(
                    FORMAT_LABELS[item.format] ?: item.format,
                    item.episodes?.let { "${it} ep" },
                    (item.averageScore ?: item.meanScore)?.let { "★ $it" }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.genres.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.genres.take(3).joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SeasonalFilterSheet(
    filter: SeasonalFilter,
    availableGenres: List<String>,
    availableFormats: List<String>,
    onGenreToggle: (String) -> Unit,
    onFormatToggle: (String) -> Unit,
    onSortChange: (SeasonalSortOrder) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filteri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClear) { Text("Resetuj") }
            }
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Sortiraj po", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SeasonalSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = filter.sortOrder == order,
                        onClick = { onSortChange(order) },
                        label = { Text(order.label) }
                    )
                }
            }

            if (availableFormats.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Format", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableFormats.forEach { fmt ->
                        FilterChip(
                            selected = fmt in filter.formats,
                            onClick = { onFormatToggle(fmt) },
                            label = { Text(FORMAT_LABELS[fmt] ?: fmt) }
                        )
                    }
                }
            }

            if (availableGenres.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Žanr", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    availableGenres.forEach { genre ->
                        FilterChip(
                            selected = genre in filter.genres,
                            onClick = { onGenreToggle(genre) },
                            label = { Text(genre) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun prevSeason(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> {
    val seasons = AnimeSeason.entries
    val idx = seasons.indexOf(season)
    return if (idx == 0) Pair(seasons.last(), year - 1)
    else Pair(seasons[idx - 1], year)
}

private fun nextSeason(season: AnimeSeason, year: Int): Pair<AnimeSeason, Int> {
    val seasons = AnimeSeason.entries
    val idx = seasons.indexOf(season)
    return if (idx == seasons.lastIndex) Pair(seasons.first(), year + 1)
    else Pair(seasons[idx + 1], year)
}
