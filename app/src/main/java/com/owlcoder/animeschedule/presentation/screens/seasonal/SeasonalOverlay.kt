package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.EmptyState
import com.owlcoder.animeschedule.presentation.components.LoadingShimmer
import com.owlcoder.animeschedule.ui.theme.PillShape

/**
 * Seasonal browser as a bottom overlay (mirrors Search/Notifications) instead of a full nav
 * destination. Reuses [SeasonalViewModel] + [SeasonalAnimeCard] + [SeasonalFilterSheet] from
 * the old full screen; season/year nav + filter trigger live in a compact inline header
 * instead of a Scaffold TopAppBar (ModalBottomSheet has no topBar slot).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonalOverlay(
    onAnimeClick: (Int) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)) {
            // Header: season/year nav + filter trigger.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTapTarget(onClick = {
                    val (prev, prevYear) = prevSeason(uiState.season, uiState.year)
                    viewModel.setSeason(prev, prevYear)
                }) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                }
                Text(
                    "${stringResource(uiState.season.labelRes())} ${uiState.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                IconTapTarget(onClick = {
                    val (next, nextYear) = nextSeason(uiState.season, uiState.year)
                    viewModel.setSeason(next, nextYear)
                }) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
                IconTapTarget(onClick = { showFilterSheet = true }) {
                    BadgedBox(badge = { if (uiState.filter.isActive) Badge() }) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = stringResource(R.string.seasonal_filter_title),
                            tint = if (uiState.filter.isActive)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SeasonTabRow(
                currentSeason = uiState.season,
                currentYear = uiState.year,
                onSelect = { s, y -> viewModel.setSeason(s, y) }
            )

            when {
                uiState.isLoading -> LoadingShimmer()
                uiState.errorRes != null -> EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.seasonal_error_title),
                    subtitle = uiState.errorRes?.let { stringResource(it) } ?: ""
                )
                uiState.filteredItems.isEmpty() -> EmptyState(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.seasonal_empty_title),
                    subtitle = stringResource(R.string.seasonal_empty_subtitle)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredItems, key = { it.anilistId }) { item ->
                        SeasonalAnimeCard(
                            item = item,
                            onClick = { onAnimeClick(item.anilistId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
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
private fun IconTapTarget(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(PillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
