package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import com.owlcoder.animeschedule.ui.theme.buildAppColorScheme
import java.util.Locale

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
    presets: List<AppearancePreset> = emptyList(),
    onSavePreset: (AppearancePreset) -> Unit = {},
    onApplyPreset: (AppearancePreset) -> Unit = {},
    onDeletePreset: (String) -> Unit = {},
    onAccentChange: (AccentColor) -> Unit = {},
) {
    var presetName by remember { mutableStateOf("") }
    val darkPreview = MaterialTheme.colorScheme.background.luminance() < .35f
    val columns = if (LocalDensity.current.fontScale > 1.2f) 2 else 3
    AppSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_appearance),
        trailingContent = {
            AppButton(stringResource(R.string.common_reset), onReset,
                variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
        },
    ) {
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        Column(
            Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppSegmentedControl(
                listOf(
                    SegmentOption(stringResource(R.string.settings_theme_system), Icons.Default.PhoneAndroid),
                    SegmentOption(stringResource(R.string.settings_theme_light), Icons.Default.LightMode),
                    SegmentOption(stringResource(R.string.settings_theme_dark), Icons.Default.DarkMode),
                ), mode.ordinal, {
                    onOptionsChange(options.copy(scheduled = false))
                    onModeChange(ThemeMode.entries[it])
                },
            )
            Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.theme_preview), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Anime Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(progress = { .65f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp), gapSize = 0.dp, drawStopIndicator = {})
                    }
                }
            }
            AppearanceSectionTitle(R.string.theme_palette, Icons.Default.Palette)
            ThemePalette.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { palette ->
                        val selected = !options.dynamicColors && options.palette == palette
                        val scheme = buildAppColorScheme(darkPreview, accent, options.copy(palette = palette, amoled = false))
                        Surface(
                            modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.large)
                                .selectable(selected, role = Role.RadioButton) { onOptionsChange(options.copy(palette = palette, dynamicColors = false)) }
                                .testTag("theme-palette-${palette.name}"),
                            shape = MaterialTheme.shapes.large, color = scheme.background,
                            border = BorderStroke(if (selected) 2.dp else .5.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(scheme.primary, scheme.surfaceContainerHighest).forEach { color ->
                                        Box(Modifier.size(18.dp).clip(MaterialTheme.shapes.small).background(color))
                                    }
                                    if (selected) Icon(Icons.Default.Check, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                                }
                                Text(stringResource(palette.labelRes()), color = scheme.onBackground, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            AppearanceToggle(R.string.theme_dynamic, R.string.theme_dynamic_hint, options.dynamicColors, Icons.Default.Wallpaper) {
                onOptionsChange(options.copy(dynamicColors = it))
            }
            HorizontalDivider()
            AppearanceSectionTitle(R.string.settings_accent, Icons.Default.Colorize)
            Text(stringResource(R.string.appearance_accent_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AccentChoices(accent, options.palette == ThemePalette.CLASSIC && !options.dynamicColors, onAccentChange)
            HorizontalDivider()
            AppearanceSectionTitle(R.string.appearance_display, Icons.Default.Tune)
            AppearanceToggle(R.string.theme_amoled, R.string.theme_amoled_hint, options.amoled, Icons.Default.Contrast) { onOptionsChange(options.copy(amoled = it)) }
            AppearanceToggle(R.string.theme_contrast, R.string.theme_contrast_hint, options.highContrast, Icons.Default.Visibility) { onOptionsChange(options.copy(highContrast = it)) }
            AppearanceToggle(R.string.theme_motion, R.string.theme_motion_hint, options.reduceMotion, Icons.Default.Animation) { onOptionsChange(options.copy(reduceMotion = it)) }
            AppearanceToggle(R.string.compact_layout, R.string.compact_layout_hint, options.compactLayout, Icons.Default.ViewCompact) { onOptionsChange(options.copy(compactLayout = it)) }
            AppearanceToggle(R.string.scheduled_theme, R.string.scheduled_theme_hint, options.scheduled, Icons.Default.Schedule) { onOptionsChange(options.copy(scheduled = it)) }
            if (options.scheduled) {
                HourSetting(R.string.dark_starts, options.darkStartHour) { onOptionsChange(options.copy(darkStartHour = it)) }
                HourSetting(R.string.dark_ends, options.darkEndHour) { onOptionsChange(options.copy(darkEndHour = it)) }
                Text(stringResource(R.string.scheduled_theme_equal_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            AppearanceSectionTitle(R.string.appearance_saved, Icons.Default.Bookmarks)
            Text(stringResource(R.string.preset_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(presetName, { presetName = it.take(32) }, Modifier.fillMaxWidth().testTag("preset-name"),
                label = { Text(stringResource(R.string.preset_name)) }, leadingIcon = { Icon(Icons.Default.Edit, null) }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboard?.hide() }),
                shape = MaterialTheme.shapes.large)
            AppButton(stringResource(R.string.preset_save), {
                onSavePreset(AppearancePreset(presetName.trim(), mode, accent, options)); presetName = ""; focusManager.clearFocus(); keyboard?.hide()
            }, Modifier.fillMaxWidth().testTag("preset-save"), enabled = presetName.isNotBlank(), icon = Icons.Default.BookmarkAdd)
            if (presets.isEmpty()) Text(stringResource(R.string.preset_empty), style = MaterialTheme.typography.bodyMedium)
            presets.forEach { preset ->
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppButton(preset.name, { focusManager.clearFocus(); keyboard?.hide(); onApplyPreset(preset) }, Modifier.weight(1f).testTag("preset-${preset.name}"),
                            variant = AppButtonVariant.Plain, icon = Icons.Default.Palette)
                        IconButton({ onDeletePreset(preset.name) }) {
                            Icon(Icons.Default.DeleteOutline, stringResource(R.string.preset_delete, preset.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSectionTitle(title: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccentChoices(accent: AccentColor, active: Boolean, onSelect: (AccentColor) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .35f
    val columns = if (LocalDensity.current.fontScale > 1.2f) 1 else 2
    AccentColor.entries.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { color ->
                AppChoiceRow(accentLabel(color), Icons.Default.Palette, active && color == accent,
                    { onSelect(color) }, Modifier.weight(1f).testTag("theme-accent-${color.name}"),
                    iconTint = com.owlcoder.animeschedule.ui.theme.accentPrimary(color, dark))
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun AppearanceToggle(title: Int, hint: Int, checked: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onChange: (Boolean) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().then(Modifier.toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange)).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppSwitch(checked, null, Modifier.testTag("appearance-switch-$title"))
        }
    }
}

@Composable
private fun HourSetting(title: Int, hour: Int, onChange: (Int) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ onChange((hour + 23) % 24) }) { Icon(Icons.Default.Remove, stringResource(R.string.hour_decrease)) }
                Text(String.format(Locale.ROOT, "%02d:00", hour), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton({ onChange((hour + 1) % 24) }) { Icon(Icons.Default.Add, stringResource(R.string.hour_increase)) }
            }
        }
    }
}

internal fun ThemePalette.labelRes(): Int = when (this) {
    ThemePalette.CLASSIC -> R.string.palette_classic
    ThemePalette.MIDNIGHT -> R.string.palette_midnight
    ThemePalette.SAKURA -> R.string.palette_sakura
    ThemePalette.FOREST -> R.string.palette_forest
    ThemePalette.SAND -> R.string.palette_sand
    ThemePalette.OCEAN -> R.string.palette_ocean
    ThemePalette.LAVENDER -> R.string.palette_lavender
    ThemePalette.EMBER -> R.string.palette_ember
    ThemePalette.ICE -> R.string.palette_ice
    ThemePalette.COFFEE -> R.string.palette_coffee
    ThemePalette.NEON -> R.string.palette_neon
}
