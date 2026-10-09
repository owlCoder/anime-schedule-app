package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.presentation.screens.discovery.DiscoveryChip

private enum class DetailToolPage { MENU, TITLES, CHARACTERS, RELATIONS, SYNOPSIS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DetailToolsSheet(detail: AnimeDetail, onDismiss: () -> Unit, onCharacter: (Int) -> Unit, onCopy: (String) -> Unit,
    onRelated: (Int) -> Unit = {}, onCopySynopsis: (String) -> Unit = onCopy, onShareProgress: (String) -> Unit = {}, onCopyLink: (String) -> Unit = onCopy) {
    var page by rememberSaveable(detail.animeId) { mutableStateOf(DetailToolPage.MENU) }
    var query by rememberSaveable(detail.animeId) { mutableStateOf("") }
    var role by rememberSaveable(detail.animeId) { mutableStateOf(CharacterRole.ALL) }
    var relatedQuery by rememberSaveable(detail.animeId) { mutableStateOf("") }
    var relation by rememberSaveable(detail.animeId) { mutableStateOf<String?>(null) }
    val related = remember(detail.relations) { detail.relations.findRelatedAnime() }
    val relationTypes = remember(detail.relations) {
        detail.relations.mapNotNull { it.relationType?.takeIf(String::isNotBlank) }.distinct().sorted()
            .filter { detail.relations.findRelatedAnime(relationType = it).isNotEmpty() }
    }
    val synopsis = rememberDescription(detail.description)
    val catalogLink = remember(detail.animeId, detail.malId, detail.malListEntry?.animeId) { detail.catalogLink() }
    val entry = detail.malListEntry
    val progressText = entry?.let {
        listOf(detail.titleRomaji ?: detail.titleEnglish.orEmpty(), it.status.displayName(),
            stringResource(R.string.detail_progress_line, it.episodesWatched, (detail.episodes ?: it.totalEpisodes)?.toString() ?: "?"),
            if (it.score > 0) stringResource(R.string.detail_progress_score, it.score) else stringResource(R.string.detail_progress_unrated),
            if (it.animeId > 0) "https://myanimelist.net/anime/${it.animeId}" else "https://anilist.co/anime/${detail.animeId}").joinToString("\n")
    }
    fun navigate(target: DetailToolPage) {
        page = target
    }
    val title = stringResource(when (page) {
        DetailToolPage.MENU -> R.string.detail_tools
        DetailToolPage.TITLES -> R.string.detail_alternative_titles
        DetailToolPage.CHARACTERS -> R.string.detail_character_finder
        DetailToolPage.RELATIONS -> R.string.detail_related_finder
        DetailToolPage.SYNOPSIS -> R.string.detail_synopsis_reader
    })
    AppSheet(onDismissRequest = onDismiss, onNavigateBack = {
        if (page != DetailToolPage.MENU) { navigate(DetailToolPage.MENU); true } else false
    }, title = title, trailingContent = {
        if (page == DetailToolPage.SYNOPSIS) GlassIconButton(Icons.Default.ContentCopy, stringResource(R.string.detail_copy_synopsis), { onCopySynopsis(synopsis) })
    }) {
        // The sheet has its own focus owner. Clear its input when navigating between pages.
        val focus = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        LaunchedEffect(page) { focus.clearFocus(force = true); keyboard?.hide() }
        when (page) {
            DetailToolPage.MENU -> LazyColumn(Modifier.heightIn(max = 580.dp).testTag("detail-tools-menu"), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                item { AppActionRow(stringResource(R.string.detail_alternative_titles), Icons.Default.Translate, { navigate(DetailToolPage.TITLES) }, Modifier.testTag("detail-titles")) }
                item { AppActionRow(stringResource(R.string.detail_character_finder), Icons.Default.PersonSearch, { navigate(DetailToolPage.CHARACTERS) }, Modifier.testTag("detail-character-finder"), enabled = detail.characters.isNotEmpty()) }
                item { AppActionRow(stringResource(R.string.detail_related_finder), Icons.Default.AccountTree, { navigate(DetailToolPage.RELATIONS) }, Modifier.testTag("detail-related-finder"), enabled = related.isNotEmpty()) }
                item { AppActionRow(stringResource(R.string.detail_read_synopsis), Icons.Default.MenuBook, { navigate(DetailToolPage.SYNOPSIS) }, Modifier.testTag("detail-read-synopsis"), enabled = synopsis.isNotBlank()) }
                item { AppActionRow(stringResource(R.string.detail_copy_synopsis), Icons.Default.ContentCopy, { onCopySynopsis(synopsis) }, Modifier.testTag("detail-copy-synopsis"), enabled = synopsis.isNotBlank(), showChevron = false) }
                item { AppActionRow(stringResource(R.string.detail_copy_link), Icons.Default.Link, { catalogLink?.let(onCopyLink) }, Modifier.testTag("detail-copy-link"), enabled = catalogLink != null, showChevron = false) }
                item { AppActionRow(stringResource(R.string.detail_share_progress), Icons.Default.Share, { progressText?.let(onShareProgress) }, Modifier.testTag("detail-share-progress"), enabled = entry != null,
                    subtitle = if (entry == null) stringResource(R.string.detail_progress_hint) else null, showChevron = false) }
                item {
                    val tools = LocalWatchTools.current
                    if (detail.animeId > 0) {
                        val muted = detail.animeId in tools.data.mutedNotifications
                        AppChoiceRow(stringResource(R.string.notifications_mute), Icons.Default.NotificationsOff, muted,
                            { tools.setNotificationMuted(detail.animeId, detail.titleRomaji ?: detail.titleEnglish.orEmpty(), !muted) },
                            Modifier.testTag("detail-mute"), subtitle = stringResource(R.string.notifications_mute_hint), selectionRole = Role.Checkbox, iconBadge = true)
                    } else Text(stringResource(R.string.notifications_mute_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DetailToolPage.SYNOPSIS -> LazyColumn(Modifier.heightIn(max = 580.dp).fillMaxWidth().testTag("synopsis-reader")) {
                item { androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(synopsis, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).testTag("synopsis-text"))
                } }
            }
            DetailToolPage.RELATIONS -> {
                val matches = remember(detail.relations, relatedQuery, relation) { detail.relations.findRelatedAnime(relatedQuery, relation) }
                LazyColumn(Modifier.heightIn(max = 580.dp).fillMaxWidth().testTag("related-results"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text(stringResource(R.string.detail_related_loaded, related.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item { AppSearchField(relatedQuery, { relatedQuery = it }, Modifier.testTag("related-search"), stringResource(R.string.detail_related_search), Icons.Default.Search, onClear = { relatedQuery = "" }) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf<String?>(null) + relationTypes).forEach { type ->
                            DiscoveryChip(if (type == null) stringResource(R.string.detail_relation_all) else relationTypeLabel(type).orEmpty(), Icons.Default.AccountTree,
                                type == relation, { relation = type; focus.clearFocus(); keyboard?.hide() }, Modifier.testTag("related-type-${type ?: "ALL"}"))
                        }
                    } }
                    if (matches.isEmpty()) item { EmptyState(Icons.Default.SearchOff, stringResource(R.string.detail_related_empty),
                        actionLabel = stringResource(R.string.filter_reset), onAction = { relatedQuery = ""; relation = null; focus.clearFocus(); keyboard?.hide() }) }
                    items(matches, key = { it.animeId }) { anime ->
                        Surface(Modifier.fillMaxWidth().clickable(role = Role.Button) {
                            focus.clearFocus(force = true); keyboard?.hide(); onRelated(anime.animeId)
                        }.testTag("finder-related-${anime.animeId}"), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MediaThumbnail.Small(anime.coverImageUrl, null, Modifier.size(44.dp, 60.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(anime.title, style = MaterialTheme.typography.titleSmall)
                                    relationTypeLabel(anime.relationType)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                }
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            DetailToolPage.TITLES -> LazyColumn(Modifier.heightIn(max = 580.dp).testTag("detail-title-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(stringResource(R.string.detail_alternative_titles_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp)) }
                val titles = listOf(R.string.detail_title_romaji to detail.titleRomaji, R.string.detail_title_english to detail.titleEnglish, R.string.detail_title_native to detail.titleNative).filter { !it.second.isNullOrBlank() }
                items(titles, key = { it.first }) { (labelRes, value) ->
                    val label = stringResource(labelRes)
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            AppButton(stringResource(R.string.detail_title_copy, label), { onCopy(value.orEmpty()) }, Modifier.fillMaxWidth().testTag("copy-title-$labelRes"), variant = AppButtonVariant.Plain, icon = Icons.Default.ContentCopy)
                        }
                    }
                }
            }
            DetailToolPage.CHARACTERS -> {
                Column(Modifier.heightIn(max = 580.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.detail_characters_loaded, detail.characters.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                    AppSearchField(query, { query = it }, Modifier.testTag("character-search"), stringResource(R.string.detail_character_search), Icons.Default.PersonSearch, onClear = { query = "" })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharacterRole.entries.filter { it == CharacterRole.ALL || detail.characters.any { character -> character.role.equals(it.name, true) } }.forEach { option ->
                            DiscoveryChip(characterRoleLabel(option), Icons.Default.Groups, option == role, { role = option; focus.clearFocus(); keyboard?.hide() }, Modifier.testTag("character-role-$option"))
                        }
                    }
                    val matches = remember(detail.characters, query, role) { detail.characters.findCharacters(query, role) }
                    LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().testTag("character-results"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (matches.isEmpty()) item { EmptyState(Icons.Default.PersonSearch, stringResource(R.string.detail_character_empty), actionLabel = stringResource(R.string.filter_reset), onAction = { query = ""; role = CharacterRole.ALL; focus.clearFocus(); keyboard?.hide() }) }
                        items(matches, key = { it.id }) { character ->
                            Surface(Modifier.fillMaxWidth().clickable(role = Role.Button) {
                                focus.clearFocus(force = true)
                                keyboard?.hide()
                                onCharacter(character.id)
                            }.testTag("finder-character-${character.id}"), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MediaThumbnail.Small(character.imageUrl, null, Modifier.size(44.dp, 60.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(character.name, style = MaterialTheme.typography.titleSmall)
                                        character.nativeName?.takeIf { it.isNotBlank() && it != character.name }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        CharacterRole.entries.firstOrNull { it.name.equals(character.role, true) }?.let { Text(characterRoleLabel(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                    }
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
internal fun characterRoleLabel(role: CharacterRole) = stringResource(when (role) {
    CharacterRole.ALL -> R.string.detail_role_all
    CharacterRole.MAIN -> R.string.detail_role_main
    CharacterRole.SUPPORTING -> R.string.detail_role_supporting
    CharacterRole.BACKGROUND -> R.string.detail_role_background
})
