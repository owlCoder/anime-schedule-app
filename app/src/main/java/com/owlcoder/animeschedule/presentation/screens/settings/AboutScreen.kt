package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.BuildConfig
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.ui.theme.PillShape

/**
 * About as a bottom-sheet overlay (mirrors Changelog/Search/Notifications) instead of a full
 * nav destination — an infrequently-opened info surface that doesn't need its own back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simple "AS" wordmark — no tile, no background, just the accent-coloured
            // monogram as a lockup above the app name.
            Text(
                text = stringResource(R.string.about_logo_monogram),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    stringResource(R.string.about_version_prefix, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(28.dp))

            // Main info card
            AboutSection {
                AboutRow(label = stringResource(R.string.about_version_label), value = BuildConfig.VERSION_NAME)
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_build_label), value = BuildConfig.VERSION_CODE.toString())
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_platform_label), value = "Android 12+")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_package_label), value = "com.owlcoder.animeschedule")
            }

            Spacer(Modifier.height(20.dp))

            // Data sources
            AboutSectionHeader(stringResource(R.string.about_section_data_sources))
            Spacer(Modifier.height(8.dp))
            AboutSection {
                AboutRow(label = stringResource(R.string.about_data_schedule), value = "AniList GraphQL API")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_data_list), value = "MyAnimeList API v2")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_data_fallback), value = "Jikan REST API")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_data_auth), value = "OAuth 2.0 + PKCE")
            }

            Spacer(Modifier.height(20.dp))

            // Tech stack
            AboutSectionHeader(stringResource(R.string.about_section_tech))
            Spacer(Modifier.height(8.dp))
            AboutSection {
                AboutRow(label = stringResource(R.string.about_tech_language), value = "Kotlin")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_tech_ui), value = "Jetpack Compose")
                AboutDivider()
                AboutRow(label = stringResource(R.string.about_tech_arch), value = "MVVM + Clean")
                AboutDivider()
                AboutRow(label = "DI", value = "Hilt")
                AboutDivider()
                AboutRow(label = "DB", value = "Room")
            }

            Spacer(Modifier.height(28.dp))

            Text(
                stringResource(R.string.about_footer_copyright, java.time.Year.now().value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AboutSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = TextUnit(1.1f, TextUnitType.Sp),
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
    )
}

@Composable
private fun AboutSection(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun AboutDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
