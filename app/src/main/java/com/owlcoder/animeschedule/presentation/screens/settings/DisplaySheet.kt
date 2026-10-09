package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisplaySheet(options: ThemeOptions, onOptionsChange: (ThemeOptions) -> Unit, onDismiss: () -> Unit) {
    AppSheet(onDismiss, title = stringResource(R.string.appearance_display), trailingContent = {
        AppButton(stringResource(R.string.common_reset), { onOptionsChange(ThemeOptions(palette = options.palette)) },
            variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 590.dp).verticalScroll(rememberScrollState()).testTag("display-content"),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
