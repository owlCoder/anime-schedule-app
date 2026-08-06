package com.owlcoder.animeschedule.presentation.screens.seasonal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.SeasonalAnimeItem
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.MediaThumbnail
import com.owlcoder.animeschedule.presentation.components.iosPressScale
import com.owlcoder.animeschedule.ui.theme.PillShape
import java.util.Locale

@Composable
internal fun SeasonalAnimeGlassCard(
    item: SeasonalAnimeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val posterShape = ContinuousRoundedShape(18.dp)
    val format = seasonalFormatLabel(item.format)
    val episodeText = item.episodes?.let { "$it ep" }
    val supporting = listOfNotNull(format, episodeText).joinToString(" · ")
    val score = (item.averageScore ?: item.meanScore)?.let {
        String.format(Locale.ROOT, "%.1f", it / 10.0)
    }

    Column(
        modifier = modifier
            .iosPressScale(interactionSource, pressedScale = 0.985f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(posterShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            MediaThumbnail.Large(
                url = item.coverImageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
            )
            score?.let { formattedScore ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = PillShape,
                    color = Color.Black.copy(alpha = 0.62f),
                    contentColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = "★ $formattedScore",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (supporting.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
