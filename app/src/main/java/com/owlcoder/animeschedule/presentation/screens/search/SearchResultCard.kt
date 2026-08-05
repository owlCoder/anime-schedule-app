package com.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.AnimeSearchResult
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import java.util.Locale

@Composable
fun SearchResultCard(
    result: AnimeSearchResult,
    onCardClick: () -> Unit,
    onEditStatus: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val displayTitle = result.title.toDisplayTitle()
    val secondaryTitle = result.titleEnglish
        ?.takeIf { it.isNotBlank() && !it.equals(result.title, ignoreCase = true) }
    val metadata = listOfNotNull(
        result.type?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.titlecase() },
        result.year,
        result.totalEpisodes?.let { "$it ep" },
    ).joinToString(" · ")
    val score = result.meanScore?.let(::formatScore)
    val cardInteraction = remember { MutableInteractionSource() }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 3.dp)
                .iosPressScale(cardInteraction, pressedScale = 0.992f),
            shape = ContinuousRoundedShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            ),
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 86.dp)
                    .padding(start = 10.dp, end = 7.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null,
                            onClick = onCardClick,
                        )
                        .semantics(mergeDescendants = true) { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    MediaThumbnail.Small(
                        url = result.coverImageUrl,
                        contentDescription = displayTitle,
                        modifier = Modifier.size(50.dp, 68.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        secondaryTitle?.let { englishTitle ->
                            Text(
                                text = englishTitle.toDisplayTitle(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (metadata.isNotEmpty() || score != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                if (metadata.isNotEmpty()) {
                                    Text(
                                        text = metadata,
                                        modifier = Modifier.weight(1f, fill = false),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                score?.let {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        tonalElevation = 0.dp,
                                    ) {
                                        Text(
                                            text = "★ $it",
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (onEditStatus != null) {
                    Spacer(Modifier.width(1.dp))
                    SearchListAction(
                        result = result,
                        onClick = onEditStatus,
                    )
                }
            }
        }
        if (showDivider) Spacer(Modifier.height(1.dp))
    }
}

@Composable
private fun SearchListAction(
    result: AnimeSearchResult,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(42.dp)
            .iosPressScale(interactionSource, pressedScale = 0.91f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
            contentColor = if (result.userListEntry == null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.60f),
            ),
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (result.userListEntry != null) {
                        Icons.Outlined.Edit
                    } else {
                        Icons.Outlined.Add
                    },
                    contentDescription = if (result.userListEntry != null) {
                        stringResource(R.string.cd_edit_list_status)
                    } else {
                        stringResource(R.string.detail_add_to_list)
                    },
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

private fun formatScore(rawScore: Double): String {
    val normalized = if (rawScore > 10.0) rawScore / 10.0 else rawScore
    return String.format(Locale.ROOT, "%.1f", normalized)
}

private fun String.toDisplayTitle(): String {
    if (isBlank()) return this
    val letters = filter(Char::isLetter)
    if (letters.isEmpty() || letters.any(Char::isLowerCase)) return this
    return lowercase().replaceFirstChar { it.titlecase() }
}
