package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(72.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = ContinuousRoundedShape(23.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.075f),
                border = BorderStroke(
                    width = 0.75.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {}
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.Center),
                shape = ContinuousRoundedShape(15.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-3).dp, y = 3.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {}
        }
        Spacer(Modifier.height(13.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                modifier = Modifier.widthIn(max = 300.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            AppButton(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.widthIn(min = 132.dp, max = 220.dp),
                variant = AppButtonVariant.Primary,
            )
        }
    }
}
