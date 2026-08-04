package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeDetail
import com.owlcoder.animeschedule.domain.model.Character
import com.owlcoder.animeschedule.domain.model.MalListEntry
import com.owlcoder.animeschedule.domain.model.RelatedAnime
import com.owlcoder.animeschedule.domain.model.WatchSource
import com.owlcoder.animeschedule.domain.model.WatchStatus
import com.owlcoder.animeschedule.presentation.components.AppButton
import com.owlcoder.animeschedule.presentation.components.AppButtonVariant
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.ErrorBanner
import com.owlcoder.animeschedule.presentation.components.FaviconImage
import com.owlcoder.animeschedule.presentation.components.GlassIconButton
import com.owlcoder.animeschedule.presentation.components.InsetGroup
import com.owlcoder.animeschedule.presentation.components.InsetListRow
import com.owlcoder.animeschedule.presentation.components.IosMotion
import com.owlcoder.animeschedule.presentation.components.ListStatusBottomSheet
import com.owlcoder.animeschedule.presentation.components.LocalMotionPolicy
import com.owlcoder.animeschedule.presentation.components.LocalToast
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.displayName
import com.owlcoder.animeschedule.presentation.components.iosSpring
import com.owlcoder.animeschedule.presentation.components.iosTween
import com.owlcoder.animeschedule.presentation.screens.settings.AuthViewModel
import com.owlcoder.animeschedule.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit = {},
    onWatchSourceClick: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
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
                            status = WatchStatus.COMPLETED,
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
    ) { innerPadding ->
        when {
            uiState.isLoading -> DetailLoadingState(Modifier.fillMaxSize().padding(innerPadding))
            uiState.errorRes != null || uiState.detail == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = onBack,
                    )
                    ErrorBanner(message = stringResource(uiState.errorRes ?: R.string.error_load_details))
                }
            }
            else -> {
                val detail = uiState.detail ?: return@Scaffold
                val sortedRelations = remember(detail.relations) { sortRelatedAnime(detail.relations) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "hero", contentType = "hero") {
                        DetailHero(detail = detail, onBack = onBack)
                    }

                    item(key = "actions", contentType = "actions") {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            DetailActions(
                                detail = detail,
                                isLoggedIn = uiState.isLoggedIn,
                                isIncrementing = uiState.isIncrementing,
                                onLogin = { authViewModel.launchMalLogin(context) },
                                onAdd = { showStatusSheet = true },
                                onEdit = { showStatusSheet = true },
                                onIncrement = viewModel::incrementEpisode,
                            )
                            detail.malListEntry?.let { ProgressPanel(detail, it) }
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        item(key = "genres", contentType = "chips") {
                            FlowRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                detail.genres.forEach { MetadataPill(it) }
                            }
                        }
                    }

                    if (uiState.watchSources.isNotEmpty()) {
                        item(key = "watch-sources", contentType = "section") {
                            WatchSourcesSection(
                                sources = uiState.watchSources,
                                animeTitle = detail.titleRomaji ?: detail.titleEnglish.orEmpty(),
                                context = context,
                                onWatchSourceClick = onWatchSourceClick,
                            )
                        }
                    }

                    detail.description
                        ?.replace(Regex("<[^>]*>"), "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { synopsis ->
                            item(key = "synopsis", contentType = "section") {
                                SynopsisSection(synopsis)
                            }
                        }

                    if (detail.characters.isNotEmpty()) {
                        item(key = "characters", contentType = "horizontal-list") {
                            HorizontalSection(title = stringResource(R.string.detail_characters)) {
                                items(
                                    items = detail.characters,
                                    key = { "character-${it.id}" },
                                    contentType = { "character" },
                                ) { character ->
                                    CharacterCard(character) { viewModel.openCharacter(character.id) }
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
                                    contentType = { "related" },
                                ) { related ->
                                    RelatedAnimeCard(related) {
                                        if (related.mediaType == null || related.mediaType == "ANIME") {
                                            onAnimeClick(related.animeId)
                                        } else {
                                            val path = related.mediaType.lowercase()
                                            val uri = android.net.Uri.parse("https://anilist.co/$path/${related.animeId}")
                                            androidx.browser.customtabs.CustomTabsIntent.Builder()
                                                .build()
                                                .launchUrl(context, uri)
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

    if (showStatusSheet && uiState.detail != null) {
        ListStatusBottomSheet(
            animeId = uiState.detail!!.animeId,
            currentEntry = finaleOverrideEntry ?: uiState.detail!!.malListEntry,
            onDismiss = {
                showStatusSheet = false
                finaleOverrideEntry = null
            },
            onConfirm = { _, update -> viewModel.updateListEntry(update) },
            onRemove = { viewModel.removeListEntry() },
        )
    }

    if (characterOverlay.isVisible) {
        CharacterOverlaySheet(state = characterOverlay, onDismiss = viewModel::dismissCharacterOverlay)
    }
}

@Composable
private fun DetailHero(detail: AnimeDetail, onBack: () -> Unit) {
    val title = detail.titleRomaji ?: detail.titleEnglish.orEmpty()
    val motion = LocalMotionPolicy.current
    var revealed by remember(detail.animeId) { mutableStateOf(false) }
    LaunchedEffect(detail.animeId) { revealed = true }
    val metadata = listOfNotNull(
        detail.format?.toDisplayFormat(),
        detail.seasonYear?.toString(),
        detail.episodes?.let { "$it ep" },
        detail.averageScore?.let { "★ ${it / 10.0}" },
    )

    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        AsyncImage(
            model = detail.bannerImageUrl ?: detail.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.42f to Color.Black.copy(alpha = 0.18f),
                        0.66f to Color.Black.copy(alpha = 0.56f),
                        0.86f to Color.Black.copy(alpha = 0.82f),
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
                onImagery = true,
            )
        }
        AnimatedVisibility(
            visible = revealed,
            modifier = Modifier.align(Alignment.BottomStart),
            enter = fadeIn(animationSpec = motion.iosTween(IosMotion.Standard)) +
                slideInVertically(
                    animationSpec = motion.iosTween(IosMotion.Standard),
                    initialOffsetY = { if (motion.animationsEnabled) it / 6 else 0 },
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                MediaThumbnail.Large(
                    url = detail.coverImageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(width = 70.dp, height = 100.dp)
                        .border(0.5.dp, Color.White.copy(alpha = 0.28f), MaterialTheme.shapes.large),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    detail.titleEnglish
                        ?.takeIf { it != detail.titleRomaji && it.isNotBlank() }
                        ?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    if (metadata.isNotEmpty()) {
                        Text(
                            text = metadata.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
    onIncrement: () -> Unit,
) {
    when {
        !isLoggedIn -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AppButton(
                label = stringResource(R.string.detail_login_cta),
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp),
                variant = AppButtonVariant.Primary,
            )
        }
        detail.malListEntry == null -> AppButton(
            label = stringResource(R.string.detail_add_to_list),
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Add,
            variant = AppButtonVariant.Primary,
        )
        else -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppButton(
                label = stringResource(R.string.detail_status),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Edit,
                variant = AppButtonVariant.Secondary,
            )
            AppButton(
                label = if (isIncrementing) "…" else "+1",
                onClick = onIncrement,
                modifier = Modifier.width(68.dp),
                enabled = detail.malListEntry.status == WatchStatus.WATCHING && !isIncrementing,
                variant = AppButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun ProgressPanel(detail: AnimeDetail, entry: MalListEntry) {
    val total = detail.episodes ?: entry.totalEpisodes
    val progress = total?.takeIf { it > 0 }
        ?.let { (entry.episodesWatched.toFloat() / it.toFloat()).coerceIn(0f, 1f) }

    InsetGroup(title = entry.status.displayName()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.episodesWatched}/${total ?: "?"} ${stringResource(R.string.detail_episodes).lowercase()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.score > 0) {
                    Text(
                        text = "${stringResource(R.string.detail_score)} ${entry.score}/10",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WatchSourcesSection(
    sources: List<WatchSource>,
    animeTitle: String,
    context: android.content.Context,
    onWatchSourceClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(stringResource(R.string.detail_watch_on))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sources.forEach { source ->
                WatchSourceChip(source) {
                    val url = source.buildUrl(animeTitle)
                    if (source.openExternally) {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url),
                            ),
                        )
                    } else onWatchSourceClick(url)
                }
            }
        }
        Text(
            text = stringResource(R.string.detail_watch_on_disclaimer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun SynopsisSection(text: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle(stringResource(R.string.detail_synopsis))
        AppMaterialSurface(
            modifier = Modifier.fillMaxWidth(),
            material = AppMaterial.Grouped,
            shape = MaterialTheme.shapes.large,
        ) {
            ExpandableSynopsis(text)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 2.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun HorizontalSection(title: String, content: LazyListScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(text = title, modifier = Modifier.padding(horizontal = 16.dp))
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.navigationBarsPadding().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(280.dp).background(MaterialTheme.colorScheme.surfaceContainer))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().height(44.dp).clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
            Box(
                Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterOverlaySheet(state: CharacterOverlayState, onDismiss: () -> Unit) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = state.detail?.name ?: stringResource(R.string.detail_characters),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
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
                                    modifier = Modifier.size(52.dp),
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
                                    text = it,
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    relations.filter { it.relationType !in hiddenRelationTypes }.sortedBy { relationSortPriority(it.relationType) }

private fun String.toDisplayFormat(): String = when (this) {
    "TV_SHORT" -> "TV Short"
    "MOVIE" -> "Movie"
    "SPECIAL" -> "Special"
    "MUSIC" -> "Music"
    else -> lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.titlecase() }
}

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

private const val SYNOPSIS_COLLAPSED_LINES = 4

@Composable
private fun ExpandableSynopsis(text: String) {
    var expanded by remember(text) { mutableStateOf(false) }
    var isOverflowing by remember(text) { mutableStateOf(false) }
    val motion = LocalMotionPolicy.current
    Column(
        modifier = Modifier
            .animateContentSize(animationSpec = motion.iosSpring())
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else SYNOPSIS_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) isOverflowing = result.hasVisualOverflow },
        )
        if (isOverflowing || expanded) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clickable { expanded = !expanded }
                    .padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun WatchSourceChip(source: WatchSource, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(38.dp).clickable(onClick = onClick),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            source.faviconUrl?.let {
                FaviconImage(
                    faviconUrl = it,
                    siteUrl = source.urlTemplate,
                    modifier = Modifier.size(18.dp).clip(CircleShape),
                )
            }
            Text(
                text = source.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CharacterCard(character: Character, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(92.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = character.name
            },
    ) {
        MediaThumbnail.Small(
            url = character.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = character.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        character.role?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RelatedAnimeCard(related: RelatedAnime, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = related.title
            },
    ) {
        relationTypeLabel(related.relationType)?.let { label ->
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        MediaThumbnail.Small(
            url = related.coverImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = related.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
