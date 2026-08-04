package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutBottomSheet(onDismiss: () -> Unit) {
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_about),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.about_logo_monogram),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))

            AboutGroup(
                items = listOf(
                    stringResource(R.string.about_version_label) to BuildConfig.VERSION_NAME,
                    stringResource(R.string.about_build_label) to BuildConfig.VERSION_CODE.toString(),
                    stringResource(R.string.about_platform_label) to "Android 12+",
                    stringResource(R.string.about_package_label) to BuildConfig.APPLICATION_ID,
                ),
            )

            Spacer(Modifier.height(14.dp))
            AboutSectionHeader(stringResource(R.string.about_section_data_sources))
            Spacer(Modifier.height(5.dp))
            AboutGroup(
                items = listOf(
                    stringResource(R.string.about_data_schedule) to "AniList",
                    stringResource(R.string.about_data_list) to "MyAnimeList",
                    stringResource(R.string.about_data_fallback) to "Jikan",
                ),
            )

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Kotlin · Jetpack Compose · Room · Hilt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_footer_copyright, java.time.Year.now().value),
                modifier = Modifier.padding(top = 5.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
    )
}

@Composable
private fun AboutGroup(items: List<Pair<String, String>>) {
    InsetGroup {
        items.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 13.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.44f),
                )
            }
        }
    }
}
