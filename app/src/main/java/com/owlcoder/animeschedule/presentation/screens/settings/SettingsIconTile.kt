package com.owlcoder.animeschedule.presentation.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.owlcoder.animeschedule.presentation.components.ContinuousRoundedShape

/** A selected palette colors both tile and glyph, including dark and AMOLED themes. */
@Composable
internal fun SettingsIconTile(icon: ImageVector, destructive: Boolean = false, modifier: Modifier = Modifier) {
    Surface(modifier.size(36.dp), shape = ContinuousRoundedShape(9.dp),
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp)) }
    }
}
