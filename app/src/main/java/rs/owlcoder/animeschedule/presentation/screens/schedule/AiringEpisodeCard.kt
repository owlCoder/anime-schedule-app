package rs.owlcoder.animeschedule.presentation.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.owlcoder.animeschedule.domain.model.AiringEpisode
import rs.owlcoder.animeschedule.domain.model.WatchStatus
import rs.owlcoder.animeschedule.presentation.components.CountdownText

@Composable
fun AiringEpisodeCard(
    episode: AiringEpisode,
    isLoggedIn: Boolean,
    onCardClick: () -> Unit,
    onIncrementEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = episode.coverColor?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier.clickable(onClick = onCardClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .height(100.dp)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent strip
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(10.dp))
            // Cover image
            AsyncImage(
                model = episode.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(12.dp))
            // Info
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Epizoda ${episode.episode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                CountdownText(episode.airingAtEpochSeconds)
                if (episode.genres.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        episode.genres.take(2).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            // +1 button
            if (isLoggedIn && episode.malListEntry?.status == WatchStatus.WATCHING) {
                FilledTonalIconButton(
                    onClick = onIncrementEpisode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "+1 epizoda", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
