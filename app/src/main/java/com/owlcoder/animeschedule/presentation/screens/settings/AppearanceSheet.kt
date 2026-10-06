package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import com.owlcoder.animeschedule.presentation.components.AppSegmentedControl
import com.owlcoder.animeschedule.presentation.components.SegmentOption
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.AppSheet
import com.owlcoder.animeschedule.ui.theme.buildAppColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSheet(
    mode: ThemeMode,
    accent: AccentColor,
    options: ThemeOptions,
    onModeChange: (ThemeMode) -> Unit,
    onOptionsChange: (ThemeOptions) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val darkPreview = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_appearance),
        trailingContent = {
            TextButton(onClick = onReset) { Text(stringResource(R.string.common_reset)) }
        }) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Changes apply immediately so this preview also matches the rest of the app.
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        stringResource(R.string.theme_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Anime Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.theme_preview_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { .65f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        gapSize = 0.dp,
                        drawStopIndicator = {})
                }
            }
            AppSegmentedControl(
                options = listOf(
                    SegmentOption(
                        stringResource(R.string.settings_theme_system),
                        Icons.Default.PhoneAndroid
                    ),
                    SegmentOption(
                        stringResource(R.string.settings_theme_light),
                        Icons.Default.LightMode
                    ),
                    SegmentOption(
                        stringResource(R.string.settings_theme_dark),
                        Icons.Default.DarkMode
                    ),
                ),
                selectedIndex = mode.ordinal, onSelect = { onModeChange(ThemeMode.entries[it]) },
            )
            Text(
                stringResource(R.string.theme_palette),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val columns = if (LocalDensity.current.fontScale > 1.2f) 2 else 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePalette.entries.chunked(columns).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { palette ->
                            val selected = !options.dynamicColors && options.palette == palette
                            val scheme = buildAppColorScheme(
                                darkPreview,
                                accent,
                                options.copy(palette = palette, amoled = false)
                            )
                            Surface(
                                modifier = Modifier.weight(1f)
                                    .clip(MaterialTheme.shapes.large)
                                    .selectable(
                                        selected,
                                        role = Role.RadioButton
                                    ) {
                                        onOptionsChange(
                                            options.copy(
                                                palette = palette,
                                                dynamicColors = false
                                            )
                                        )
                                    }
                                    .testTag("theme-palette-${palette.name}"),
                                shape = MaterialTheme.shapes.large, color = scheme.background,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (selected) 2.dp else .5.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                            ) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            scheme.primary,
                                            scheme.surfaceContainerHighest
                                        ).forEach { color ->
                                            Box(
                                                Modifier.size(20.dp)
                                                    .clip(MaterialTheme.shapes.small)
                                                    .background(color)
                                            )
                                        }
                                        if (selected) Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = scheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        stringResource(palette.labelRes()),
                                        color = scheme.onBackground,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            AppearanceToggle(
                R.string.theme_dynamic,
                R.string.theme_dynamic_hint,
                options.dynamicColors
            ) { onOptionsChange(options.copy(dynamicColors = it)) }
            AppearanceToggle(
                R.string.theme_amoled,
                R.string.theme_amoled_hint,
                options.amoled
            ) { onOptionsChange(options.copy(amoled = it)) }
            AppearanceToggle(
                R.string.theme_contrast,
                R.string.theme_contrast_hint,
                options.highContrast
            ) { onOptionsChange(options.copy(highContrast = it)) }
            AppearanceToggle(
                R.string.theme_motion,
                R.string.theme_motion_hint,
                options.reduceMotion
            ) { onOptionsChange(options.copy(reduceMotion = it)) }
        }
    }
}

@Composable
private fun AppearanceToggle(title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked, onChange)
    }
}

internal fun ThemePalette.labelRes(): Int = when (this) {
    ThemePalette.CLASSIC -> R.string.palette_classic
    ThemePalette.MIDNIGHT -> R.string.palette_midnight
    ThemePalette.SAKURA -> R.string.palette_sakura
    ThemePalette.FOREST -> R.string.palette_forest
    ThemePalette.SAND -> R.string.palette_sand
}
