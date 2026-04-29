package rs.owlcoder.animeschedule.presentation.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    ElevatedCard(modifier = modifier.clickable(onClick = onCardClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = result.coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp, 70.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                result.titleEnglish?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                val meta = listOfNotNull(
                    result.type,
                    result.year,
                    result.totalEpisodes?.let { "${it}ep" },
                    result.meanScore?.let { "★ ${"%.1f".format(it)}" }
                ).joinToString(" • ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                result.userListEntry?.let { entry ->
                    AssistChip(
                        onClick = onEditStatus,
                        label = { Text(entry.status.displayName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEditStatus) {
                Icon(
                    if (result.userListEntry != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (result.userListEntry != null) "Uredi" else "Dodaj"
                )
            }
        }
    }
}
