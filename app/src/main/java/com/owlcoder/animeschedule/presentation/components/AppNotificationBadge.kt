package com.owlcoder.animeschedule.presentation.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AppNotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val clamped = count.coerceAtLeast(0)
    BadgedBox(
        modifier = modifier.then(
            if (contentDescription != null) Modifier.semantics {
                this.contentDescription = contentDescription
            } else Modifier
        ),
        badge = {
            if (clamped > 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Text(if (clamped > maxCount) "$maxCount+" else clamped.toString())
                }
            }
        },
    ) { content() }
}
