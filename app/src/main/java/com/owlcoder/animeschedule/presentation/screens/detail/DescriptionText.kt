package com.owlcoder.animeschedule.presentation.screens.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.text.HtmlCompat

/** Decode once per description; keep entities and paragraph breaks readable in every view. */
@Composable
internal fun rememberDescription(raw: String?): String = remember(raw) {
    HtmlCompat.fromHtml(raw.orEmpty(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
}
