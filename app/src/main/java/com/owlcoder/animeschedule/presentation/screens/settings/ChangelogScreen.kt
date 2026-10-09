package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.material.icons.outlined.Palette

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppMaterial
import com.owlcoder.animeschedule.presentation.components.AppMaterialSurface
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape
import com.owlcoder.animeschedule.presentation.components.InsetGroup

private data class ReleaseNote(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogBottomSheet(onDismiss: () -> Unit) {
    val notes = listOf(
        ReleaseNote(Icons.Outlined.Search, stringResource(R.string.changelog_5128_search_title), stringResource(R.string.changelog_5128_search)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5128_controls_title), stringResource(R.string.changelog_5128_controls)),
        ReleaseNote(Icons.Outlined.Palette, stringResource(R.string.changelog_5127_themes_title), stringResource(R.string.changelog_5127_themes)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5127_display_title), stringResource(R.string.changelog_5127_display)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5126_library_title), stringResource(R.string.changelog_5126_library)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5126_overlays_title), stringResource(R.string.changelog_5126_overlays)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5126_notifications_title), stringResource(R.string.changelog_5126_notifications)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5125_tabs_title), stringResource(R.string.changelog_5125_tabs)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5124_search_title), stringResource(R.string.changelog_5124_search)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5123_detail_title), stringResource(R.string.changelog_5123_detail)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5122_sources_title), stringResource(R.string.changelog_5122_sources)),
        ReleaseNote(Icons.Outlined.EventAvailable, stringResource(R.string.changelog_5122_language_title), stringResource(R.string.changelog_5122_language)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5122_controls_title), stringResource(R.string.changelog_5122_controls)),
        ReleaseNote(Icons.Outlined.EventAvailable, stringResource(R.string.changelog_5121_home_title), stringResource(R.string.changelog_5121_home)),
        ReleaseNote(Icons.Outlined.FavoriteBorder, stringResource(R.string.changelog_5121_blur_title), stringResource(R.string.changelog_5121_blur)),
        ReleaseNote(Icons.Outlined.EventAvailable, stringResource(R.string.changelog_5121_season_title), stringResource(R.string.changelog_5121_season)),
        ReleaseNote(Icons.Outlined.Tune, stringResource(R.string.changelog_5121_controls_title), stringResource(R.string.changelog_5121_controls)),
        ReleaseNote(
            Icons.Outlined.Bookmarks,
            stringResource(R.string.changelog_5120_library_title),
            stringResource(R.string.changelog_5120_library),
        ),
        ReleaseNote(
            Icons.Outlined.Timer,
            stringResource(R.string.changelog_5120_planner_title),
            stringResource(R.string.changelog_5120_planner),
        ),
        ReleaseNote(
            Icons.Outlined.BarChart,
            stringResource(R.string.changelog_5120_activity_title),
            stringResource(R.string.changelog_5120_activity),
        ),
        ReleaseNote(
            Icons.Outlined.EventAvailable,
            stringResource(R.string.changelog_5120_schedule_title),
            stringResource(R.string.changelog_5120_schedule),
        ),
        ReleaseNote(
            Icons.Outlined.Search,
            stringResource(R.string.changelog_5120_search_title),
            stringResource(R.string.changelog_5120_search),
        ),
    )

    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.changelog_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(ContinuousRoundedShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(R.string.changelog_release_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AppMaterialSurface(
                modifier = Modifier.fillMaxWidth(),
                material = AppMaterial.Interactive,
                shape = ContinuousRoundedShape(18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.03f),
                                ),
                            ),
                        )
                        .padding(horizontal = 15.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_whats_new),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.changelog_5128_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ChangelogGlassTile(size = 54.dp, cornerRadius = 16.dp) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            InsetGroup {
                notes.forEachIndexed { index, note ->
                    ReleaseNoteRow(note)
                    if (index < notes.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 58.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.changelog_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ReleaseNoteRow(note: ReleaseNote) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ChangelogGlassTile(size = 36.dp, cornerRadius = 11.dp) {
            Icon(
                imageVector = note.icon,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = note.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChangelogGlassTile(
    size: Dp,
    cornerRadius: Dp,
    content: @Composable () -> Unit,
) {
    AppMaterialSurface(
        modifier = Modifier.size(size),
        material = AppMaterial.Interactive,
        shape = ContinuousRoundedShape(cornerRadius),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
