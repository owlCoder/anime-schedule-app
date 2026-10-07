package com.owlcoder.animeschedule.presentation.screens.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.owlcoder.animeschedule.R
import java.util.Locale

@Composable
internal fun DiscoverySection(title: String) {
    Text(title, Modifier.padding(start = 4.dp, top = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

/** Intrinsic width and flexible height keep every label readable at enlarged font sizes. */
@Composable
internal fun DiscoveryChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, multiple: Boolean = false) {
    val shape = MaterialTheme.shapes.medium
    val input = if (multiple) Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
        else Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
    Surface(modifier.clip(shape).then(input), shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(.6.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .5f) else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)) {
            Icon(if (selected && multiple) Icons.Default.Check else icon, null, Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
internal fun discoveryFormatLabel(format: String?): String? = when (format?.uppercase(Locale.ROOT)) {
    "TV" -> stringResource(R.string.format_tv)
    "TV_SHORT", "TV SHORT" -> stringResource(R.string.format_tv_short)
    "MOVIE" -> stringResource(R.string.format_movie)
    "SPECIAL" -> stringResource(R.string.format_special)
    "OVA" -> stringResource(R.string.format_ova)
    "ONA" -> stringResource(R.string.format_ona)
    "MUSIC" -> stringResource(R.string.format_music)
    null -> null
    else -> format.replace('_', ' ').lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
}
