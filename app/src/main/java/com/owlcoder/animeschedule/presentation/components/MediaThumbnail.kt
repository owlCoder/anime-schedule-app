package com.owlcoder.animeschedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

object MediaThumbnail {
    @Composable
    fun Small(
        url: String?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Thumbnail(
            url = url,
            contentDescription = contentDescription,
            modifier = modifier.widthIn(min = 56.dp).heightIn(min = 72.dp),
            placeholderColor = placeholderColor,
        )
    }

    @Composable
    fun Large(
        url: String?,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Thumbnail(
            url = url,
            contentDescription = contentDescription,
            modifier = modifier.widthIn(min = 96.dp).heightIn(min = 128.dp),
            placeholderColor = placeholderColor,
        )
    }
}

@Composable
private fun Thumbnail(
    url: String?,
    contentDescription: String?,
    modifier: Modifier,
    placeholderColor: Color,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
