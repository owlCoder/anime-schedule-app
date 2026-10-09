package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.domain.model.*
import com.owlcoder.animeschedule.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSheet(
    mode: ThemeMode,
    options: ThemeOptions,
    onModeChange: (ThemeMode) -> Unit,
    onPaletteChange: (ThemePalette) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(onDismiss, title = stringResource(R.string.settings_appearance), trailingContent = {
        AppButton(stringResource(R.string.common_reset), onReset, variant = AppButtonVariant.Plain, icon = Icons.Default.RestartAlt)
    }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).testTag("appearance-content"),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeModePicker(mode, onModeChange)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.theme_palette), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.appearance_palette_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ThemePaletteGrid(options.palette, onPaletteChange)
        }
    }
}
