package rs.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import rs.owlcoder.animeschedule.domain.model.AnimeSearchResult

@Composable
fun SearchResultCard(
    result: AnimeSearchResult,
    onCardClick: () -> Unit,
    onEditStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onCardClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = result.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                result.titleEnglish?.takeIf { it != result.title }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                val meta = listOfNotNull(
                    result.type,
                    result.year,
                    result.totalEpisodes?.let { "${it} ep" },
                    result.meanScore?.let { "★ ${"%.1f".format(it)}" }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                result.userListEntry?.let { entry ->
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onEditStatus)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            entry.status.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onEditStatus,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (result.userListEntry != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (result.userListEntry != null) "Uredi" else "Dodaj",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
