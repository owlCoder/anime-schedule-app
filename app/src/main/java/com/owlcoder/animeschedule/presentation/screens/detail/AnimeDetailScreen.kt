package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.Character
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.RelatedAnime
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.FaviconImage
import com.owlcoder.animeschedule.presentation.components.GlassButton
import com.owlcoder.animeschedule.presentation.components.AppInlineHeader
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit = {},
    onWatchSourceClick: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val characterOverlay by viewModel.characterOverlay.collectAsState()
    var showStatusSheet by remember { mutableStateOf(false) }
    var finaleOverrideEntry by remember { mutableStateOf<MalListEntry?>(null) }
    val context = LocalContext.current
    val toast = LocalToast.current
    val savedMsg = stringResource(R.string.toast_status_saved)
    val markedMsg = stringResource(R.string.toast_episode_marked)
    val removedMsg = stringResource(R.string.toast_removed_from_list)
    val errorMsg = stringResource(R.string.toast_update_error)

    LaunchedEffect(Unit) {
        viewModel.updateEvent.collect { event ->
            when (event) {
                DetailViewModel.UpdateEvent.Success -> toast.success(savedMsg)
                DetailViewModel.UpdateEvent.Incremented -> {
                    toast.success(markedMsg)
                    val detail = uiState.detail
                    val entry = detail?.malListEntry
                    val total = detail?.episodes
                    if (entry != null && total != null && entry.episodesWatched + 1 >= total) {
                        finaleOverrideEntry = entry.copy(
                            episodesWatched = total,
                            status = WatchStatus.COMPLETED
                        )
                        showStatusSheet = true
                    }
                }
                DetailViewModel.UpdateEvent.Removed -> toast.success(removedMsg)
                DetailViewModel.UpdateEvent.Error -> toast.error(errorMsg)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppInlineHeader(
                title = "Anime",
                onBack = onBack,
                backContentDescription = stringResource(R.string.cd_back),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> DetailLoadingState(Modifier.fillMaxSize().padding(innerPadding))
            uiState.errorRes != null || uiState.detail == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                ErrorBanner(
                    message = stringResource(uiState.errorRes ?: R.string.error_load_details),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            else -> {
                val detail = uiState.detail ?: return@Scaffold
                val sortedRelations = remember(detail.relations) {
                    sortRelatedAnime(detail.relations)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "hero", contentType = "hero") {
                        DetailHero(detail)
                    }

                    item(key = "actions", contentType = "actions") {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailActions(
                                detail = detail,
                                isLoggedIn = uiState.isLoggedIn,
                                isIncrementing = uiState.isIncrementing,
                                onLogin = { authViewModel.launchMalLogin(context) },
                                onAdd = { showStatusSheet = true },
                                onEdit = { showStatusSheet = true },
                                onIncrement = viewModel::incrementEpisode
                            )
                            detail.malListEntry?.let { entry ->
                                ProgressPanel(detail = detail, entry = entry)
                            }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        item(key = "genres", contentType = "chips") {
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                detail.genres.forEach { genre ->
                                    MetadataPill(genre)
                                }
                            }
                        }
                    }

                    if (uiState.watchSources.isNotEmpty()) {
                        item(key = "watch-sources", contentType = "section") {
                            WatchSourcesSection(
                                sources = uiState.watchSources,
                                animeTitle = detail.titleRomaji ?: detail.titleEnglish.orEmpty(),
                                context = context,
                                onWatchSourceClick = onWatchSourceClick
                            )
                        }
                    }

                    detail.description
                        ?.replace(Regex("<[^>]*>"), "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { synopsis ->
                            item(key = "synopsis", contentType = "section") {
                                DetailSection(title = stringResource(R.string.detail_synopsis)) {
                                    ExpandableSynopsis(synopsis)
                                }
                            }
                        }

                    if (detail.characters.isNotEmpty()) {
                        item(key = "characters", contentType = "horizontal-list") {
                            HorizontalSection(title = stringResource(R.string.detail_characters)) {
                                items(
                                    items = detail.characters,
                                    key = { "character-${it.id}" },
                                    contentType = { "character" }
                                ) { character ->
                                    CharacterCard(
                                        character = character,
                                        onClick = { viewModel.openCharacter(character.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (sortedRelations.isNotEmpty()) {
                        item(key = "related", contentType = "horizontal-list") {
                            HorizontalSection(title = stringResource(R.string.detail_related)) {
                                items(
                                    items = sortedRelations,
                                    key = { "related-${it.animeId}" },
                                    contentType = { "related" }
                                ) { related ->
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
            }
        }
    }

    if (showStatusSheet && uiState.detail != null) {
        ListStatusBottomSheet(
            animeId = uiState.detail!!.animeId,
            currentEntry = finaleOverrideEntry ?: uiState.detail!!.malListEntry,
            onDismiss = {
                showStatusSheet = false
                finaleOverrideEntry = null
            },
            onConfirm = { _, update -> viewModel.updateListEntry(update) },
            onRemove = { viewModel.removeListEntry() }
        )
    }

    if (characterOverlay.isVisible) {
        CharacterOverlaySheet(
            state = characterOverlay,
            onDismiss = viewModel::dismissCharacterOverlay
        )
    }
}

@Composable
private fun DetailHero(detail: AnimeDetail) {
    val title = detail.titleRomaji ?: detail.titleEnglish.orEmpty()
    val metadata = listOfNotNull(
        detail.format,
        detail.seasonYear?.toString(),
        detail.episodes?.let { "$it ep" },
        detail.averageScore?.let { "★ ${it / 10.0}" }
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
    ) {
        AsyncImage(
            model = detail.bannerImageUrl ?: detail.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MediaThumbnail.Large(
                url = detail.coverImageUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(width = 76.dp, height = 108.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.24f), MaterialTheme.shapes.medium),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                detail.titleEnglish
                    ?.takeIf { it != detail.titleRomaji && it.isNotBlank() }
                    ?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                detail.titleNative
                    ?.takeIf { it.isNotBlank() && it != detail.titleEnglish }
                    ?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                if (metadata.isNotEmpty()) {
                    Text(
                        metadata.joinToString("  •  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailActions(
    detail: AnimeDetail,
    isLoggedIn: Boolean,
    isIncrementing: Boolean,
    onLogin: () -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onIncrement: () -> Unit
) {
    if (!isLoggedIn) {
        OutlinedButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                stringResource(R.string.detail_login_cta),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    } else if (detail.malListEntry == null) {
        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.detail_add_to_list), fontWeight = FontWeight.SemiBold)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_status), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onIncrement,
                enabled = detail.malListEntry.status == WatchStatus.WATCHING && !isIncrementing,
                modifier = Modifier
                    .width(86.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
            ) {
                if (isIncrementing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("+1", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProgressPanel(detail: AnimeDetail, entry: MalListEntry) {
    val total = detail.episodes ?: entry.totalEpisodes
    val progress = total?.takeIf { it > 0 }
        ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
    InsetGroup(title = entry.status.displayName()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${entry.episodesWatched}/${total ?: "?"} ${stringResource(R.string.detail_episodes).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.score > 0) {
                    Text(
                        "${stringResource(R.string.detail_score)}  ${entry.score}/10",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    InsetGroup(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = title
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun WatchSourcesSection(
    sources: List<WatchSource>,
    animeTitle: String,
    context: android.content.Context,
    onWatchSourceClick: (String) -> Unit
) {
    DetailSection(title = stringResource(R.string.detail_watch_on)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sources.forEach { source ->
                WatchSourceChip(
                    source = source,
                    onClick = {
                        val url = source.buildUrl(animeTitle)
                        if (source.openExternally) {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(url)
                                )
                            )
                        } else {
                            onWatchSourceClick(url)
                        }
                    }
                )
            }
        }
        Text(
            stringResource(R.string.detail_watch_on_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HorizontalSection(
    title: String,
    content: LazyListScope.() -> Unit
) {
    InsetGroup(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = title
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun DetailLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(272.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterOverlaySheet(
    state: CharacterOverlayState,
    onDismiss: () -> Unit
) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = state.detail?.name ?: stringResource(R.string.detail_characters),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                state.isLoading -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.errorRes != null -> ErrorBanner(stringResource(state.errorRes))
                state.detail != null -> {
                    val detail = state.detail
                    InsetListRow(
                        label = detail.name,
                        supportingText = detail.nativeName?.takeIf { it.isNotBlank() },
                        leadingContent = {
                        detail.imageUrl?.let { imageUrl ->
                            MediaThumbnail.Small(
                                url = imageUrl,
                                contentDescription = detail.name,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                        },
                    )
                    detail.description
                        ?.replace(Regex("<[^>]*>"), "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            InsetGroup {
                                Text(
                                    it,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                }
            }
        }
    }
}

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

private const val SYNOPSIS_COLLAPSED_LINES = 5

@Composable
private fun ExpandableSynopsis(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    var isOverflowing by remember(text) { mutableStateOf(false) }
    Column {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else SYNOPSIS_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) isOverflowing = result.hasVisualOverflow
            }
        )
        if (isOverflowing || expanded) {
            Text(
                stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { expanded = !expanded }
                    .padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun WatchSourceChip(source: WatchSource, onClick: () -> Unit) {
    GlassButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) { color ->
        source.faviconUrl?.let {
            FaviconImage(
                faviconUrl = it,
                siteUrl = source.urlTemplate,
                modifier = Modifier.size(20.dp).clip(CircleShape)
            )
        }
        Text(
            source.name,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CharacterCard(character: Character, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = character.name
            }
    ) {
        MediaThumbnail.Small(
            url = character.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            character.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        character.role?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RelatedAnimeCard(related: RelatedAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = related.title
            }
    ) {
        relationTypeLabel(related.relationType)?.let { label ->
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(7.dp))
        }
        MediaThumbnail.Small(
            url = related.coverImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            related.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
