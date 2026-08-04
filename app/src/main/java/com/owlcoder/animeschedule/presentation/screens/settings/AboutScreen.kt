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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.presentation.components.InsetGroup

private data class AboutItem(
    val label: String,
    val value: String,
    val codeValue: Boolean = false,
)

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
                .heightIn(max = 450.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.about_logo_monogram),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                modifier = Modifier.padding(top = 1.dp, start = 12.dp, end = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))

            AboutGroup(
                items = listOf(
                    AboutItem(stringResource(R.string.about_version_label), BuildConfig.VERSION_NAME),
                    AboutItem(stringResource(R.string.about_build_label), BuildConfig.VERSION_CODE.toString()),
                    AboutItem(stringResource(R.string.about_platform_label), "Android 12+"),
                    AboutItem(
                        stringResource(R.string.about_package_label),
                        BuildConfig.APPLICATION_ID,
                        codeValue = true,
                    ),
                ),
            )

            Spacer(Modifier.height(9.dp))
            AboutSectionHeader(stringResource(R.string.about_section_data_sources))
            Spacer(Modifier.height(4.dp))
            AboutGroup(
                items = listOf(
                    AboutItem(stringResource(R.string.about_data_schedule), "AniList"),
                    AboutItem(stringResource(R.string.about_data_list), "MyAnimeList"),
                    AboutItem(stringResource(R.string.about_data_fallback), "Jikan"),
                ),
            )

            Spacer(Modifier.height(9.dp))
            Text(
                text = "Kotlin · Jetpack Compose · Room · Hilt",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_footer_copyright, java.time.Year.now().value),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 11.dp),
    )
}

@Composable
private fun AboutGroup(items: List<AboutItem>) {
    InsetGroup {
        items.forEachIndexed { index, item ->
            if (item.codeValue) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.46f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.54f),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
