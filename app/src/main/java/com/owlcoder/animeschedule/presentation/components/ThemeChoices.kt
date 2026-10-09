package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.ui.theme.buildAppColorScheme

/** The same fixed theme choices are used by Settings and onboarding. */
@Composable
fun ThemeModePicker(mode: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    AppSegmentedControl(listOf(
        SegmentOption(stringResource(R.string.appearance_mode_system), Icons.Default.PhoneAndroid),
        SegmentOption(stringResource(R.string.settings_theme_light), Icons.Default.LightMode),
        SegmentOption(stringResource(R.string.settings_theme_dark), Icons.Default.DarkMode),
    ), mode.ordinal, { onSelect(ThemeMode.entries[it]) }, modifier.testTag("theme-mode-picker"), compact = true)
}

@Composable
fun ThemePaletteGrid(selected: ThemePalette, onSelect: (ThemePalette) -> Unit, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < .35f
    val largeText = LocalDensity.current.fontScale > 1.2f
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (largeText || maxWidth < 340.dp) 2 else 3
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePalette.entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { palette ->
                        val scheme = remember(dark, palette) { buildAppColorScheme(dark, ThemeOptions(palette = palette, amoled = false)) }
                        val interaction = remember { MutableInteractionSource() }
                        val active = selected == palette
                        Surface(
                            Modifier.weight(1f).fillMaxHeight().clip(MaterialTheme.shapes.medium)
                                .selectable(active, interaction, indication = null, role = Role.RadioButton, onClick = { onSelect(palette) })
                                .testTag("theme-palette-${palette.name}"),
                            color = scheme.surface, shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(if (active) 1.5.dp else .5.dp,
                                if (active) scheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(Modifier.heightIn(min = 60.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(scheme.primary, scheme.primaryContainer, scheme.surfaceContainerHighest).forEach { color ->
                                        Box(Modifier.size(12.dp).clip(MaterialTheme.shapes.small).background(color))
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (active) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = scheme.primary)
                                    else Spacer(Modifier.size(16.dp))
                                }
                                Text(stringResource(palette.labelRes()), color = scheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

fun ThemePalette.labelRes(): Int = when (this) {
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
    ThemePalette.RUBY -> R.string.palette_ruby
    ThemePalette.AMETHYST -> R.string.palette_amethyst
    ThemePalette.MINT -> R.string.palette_mint
    ThemePalette.GOLD -> R.string.palette_gold
}
