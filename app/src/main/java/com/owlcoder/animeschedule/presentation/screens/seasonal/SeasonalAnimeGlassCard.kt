package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import java.util.Locale

@Composable
internal fun SeasonalAnimeGlassCard(
    item: SeasonalAnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = seasonalFormatLabel(item.format)
    val episodeText = item.episodes?.let { "$it ep" }
    val supporting = listOfNotNull(format, episodeText).joinToString(" · ")
    val score = (item.averageScore ?: item.meanScore)?.let {
        String.format(Locale.ROOT, "%.1f", it / 10.0)
    }

    AppMaterialSurface(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        material = AppMaterial.Interactive,
        shape = ContinuousRoundedShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MediaThumbnail.Large(
                url = item.coverImageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (supporting.isNotEmpty()) {
                        Text(
                            text = supporting,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    score?.let {
                        Text(
                            text = "★ $it",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun seasonalFormatLabel(format: String?): String? = when (format?.uppercase()) {
    "TV" -> stringResource(R.string.format_tv)
    "TV_SHORT" -> stringResource(R.string.format_tv_short)
    "MOVIE" -> stringResource(R.string.format_movie)
    "SPECIAL" -> stringResource(R.string.format_special)
    "OVA" -> stringResource(R.string.format_ova)
    "ONA" -> stringResource(R.string.format_ona)
    "MUSIC" -> stringResource(R.string.format_music)
    null -> null
    else -> format.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}
