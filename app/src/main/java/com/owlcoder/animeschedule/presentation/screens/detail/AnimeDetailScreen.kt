package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.RelatedAnime
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalNavBarHeight
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    var showStatusSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val toast = LocalToast.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                is DetailViewModel.UpdateEvent.Success -> toast.success(savedMsg)
                is DetailViewModel.UpdateEvent.Removed -> toast.success(removedMsg)
                is DetailViewModel.UpdateEvent.Error -> toast.error(errorMsg)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            uiState.errorRes != null -> Box(Modifier.padding(innerPadding)) {
                ErrorBanner(stringResource(uiState.errorRes!!))
            }
            else -> {
                val detail = uiState.detail ?: return@Scaffold
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero banner with gradient overlay for text legibility
                    Box(Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = detail.bannerImageUrl ?: detail.coverImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 60f
                                    )
                                )
                        )
                    }

                    // Info card "floats" over the banner via negative offset — overlap effect
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-28).dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                detail.titleRomaji ?: detail.titleEnglish ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis
                            )
                            detail.titleEnglish?.takeIf { it != detail.titleRomaji }?.let {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            detail.titleNative?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(14.dp))

                            // Meta row
                            val meta = listOfNotNull(
                                detail.format,
                                detail.seasonYear?.toString(),
                                detail.episodes?.let { "$it ep." },
                                detail.averageScore?.let { "★ ${it / 10.0}" }
                            ).joinToString("  •  ")
                            if (meta.isNotEmpty()) {
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            val mainStudio = detail.studios.firstOrNull { it.isMain }?.name
                            if (mainStudio != null) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        mainStudio,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            val entry = detail.malListEntry
                            Spacer(Modifier.height(12.dp))
                            if (!uiState.isLoggedIn) {
                                // No MAL session — a tap on "Add to list" would just 401, so
                                // offer the login flow instead.
                                GlassButton(
                                    onClick = { authViewModel.launchMalLogin(context) },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                                ) { contentColor ->
                                    Text(
                                        stringResource(R.string.detail_login_cta),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (entry != null) {
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { showStatusSheet = true }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "${entry.status.displayName()}  •  ${entry.episodesWatched}/${detail.episodes ?: "?"} ep",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.cd_edit_list_status),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                GlassButton(
                                    onClick = { showStatusSheet = true },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                                ) { contentColor ->
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        stringResource(R.string.detail_add_to_list),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (detail.genres.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    detail.genres.forEach { genre ->
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                genre,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Note: the info card's negative `offset` above only shifts its
                    // paint position, not its reserved layout slot, so the natural
                    // gap left below it already reads as intentional breathing room.
                    val contentSpacing = 12.dp

                    // Description card
                    if (!detail.description.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    stringResource(R.string.detail_synopsis),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    detail.description.replace(Regex("<[^>]*>"), ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(contentSpacing))
                    }

                    // Studio card
                    if (detail.studios.any { it.isMain }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    stringResource(R.string.detail_studios),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                detail.studios.filter { it.isMain }.forEach {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        it.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(contentSpacing))
                    }

                    // Related anime card (prequel/sequel/etc.) — sorted so prequel/sequel
                    // (the most useful "what to watch before/after" info) come first.
                    val sortedRelations = remember(detail.relations) {
                        sortRelatedAnime(detail.relations)
                    }
                    if (sortedRelations.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(vertical = 20.dp)) {
                                Text(
                                    stringResource(R.string.detail_related),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(sortedRelations, key = { it.animeId }) { related ->
                                        RelatedAnimeCard(
                                            related = related,
                                            onClick = {
                                                if (related.mediaType == null || related.mediaType == "ANIME") {
                                                    onAnimeClick(related.animeId)
                                                } else {
                                                    val path = related.mediaType.lowercase()
                                                    val uri = android.net.Uri.parse(
                                                        "https://anilist.co/$path/${related.animeId}"
                                                    )
                                                    androidx.browser.customtabs.CustomTabsIntent.Builder()
                                                        .build()
                                                        .launchUrl(context, uri)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(LocalNavBarHeight.current + 28.dp))
                }
            }
        }
    }

    if (showStatusSheet && uiState.detail != null) {
        ListStatusBottomSheet(
            animeId = uiState.detail!!.animeId,
            currentEntry = uiState.detail!!.malListEntry,
            onDismiss = { showStatusSheet = false },
            onConfirm = { _, update -> viewModel.updateListEntry(update) },
            onRemove = { viewModel.removeListEntry() }
        )
    }
}

// Relation types that aren't useful for the user in this context (e.g. character
// appearances or "contains" bundles) are filtered out; everything else is kept,
// with PREQUEL/SEQUEL always surfaced first since that's what users care about most.
private val hiddenRelationTypes = setOf("CHARACTER", "CONTAINS", "OTHER")

private fun relationSortPriority(type: String?): Int = when (type) {
    "SEQUEL" -> 0
    "PREQUEL" -> 1
    else -> 2
}

private fun sortRelatedAnime(relations: List<RelatedAnime>): List<RelatedAnime> =
    relations
        .filter { it.relationType !in hiddenRelationTypes }
        .sortedBy { relationSortPriority(it.relationType) }

@Composable
private fun relationTypeLabel(type: String?): String? = when (type) {
    "PREQUEL" -> stringResource(R.string.relation_prequel)
    "SEQUEL" -> stringResource(R.string.relation_sequel)
    "SIDE_STORY" -> stringResource(R.string.relation_side_story)
    "SPIN_OFF" -> stringResource(R.string.relation_spin_off)
    "ALTERNATIVE" -> stringResource(R.string.relation_alternative)
    "SUMMARY" -> stringResource(R.string.relation_summary)
    "ADAPTATION" -> stringResource(R.string.relation_adaptation)
    "PARENT" -> stringResource(R.string.relation_parent)
    "COMPILATION" -> stringResource(R.string.relation_compilation)
    null -> null
    else -> type.takeIf { it.isNotBlank() }
}

@Composable
private fun RelatedAnimeCard(
    related: RelatedAnime,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick)
    ) {
        val label = relationTypeLabel(related.relationType)
        if (label != null) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        AsyncImage(
            model = related.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            related.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
