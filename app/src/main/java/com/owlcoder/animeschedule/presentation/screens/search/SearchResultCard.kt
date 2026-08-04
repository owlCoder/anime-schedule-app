package com.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.owlcoder.animeschedule.presentation.components.GlassSurface
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.ui.theme.GlassBlur
import com.owlcoder.animeschedule.ui.theme.GlassTone

@Composable
fun SearchResultCard(
    result: AnimeSearchResult,
    onCardClick: () -> Unit,
    onEditStatus: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp)
                .clickable(onClick = onCardClick)
                .semantics(mergeDescendants = true) { role = Role.Button }
                .padding(start = 11.dp, end = 7.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MediaThumbnail.Small(
                url = result.coverImageUrl,
                contentDescription = result.title,
                modifier = Modifier.size(43.dp, 57.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                result.titleEnglish?.takeIf { it != result.title }?.let { englishTitle ->
                    Text(
                        text = englishTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onEditStatus != null) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(onClick = onEditStatus)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    GlassSurface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        tone = if (result.userListEntry != null) GlassTone.Neutral else GlassTone.Accent,
                        blur = GlassBlur.None,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (result.userListEntry != null) Icons.Outlined.Edit else Icons.Outlined.Add,
                                contentDescription = if (result.userListEntry != null) {
                                    stringResource(R.string.cd_edit_list_status)
                                } else {
                                    stringResource(R.string.detail_add_to_list)
                                },
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f),
            )
        }
    }
}
