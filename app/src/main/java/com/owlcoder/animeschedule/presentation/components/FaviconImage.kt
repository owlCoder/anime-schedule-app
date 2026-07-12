package com.owlcoder.animeschedule.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/** Favicon with a same-site fallback: Google's favicon service silently returns a blank
 *  placeholder for sites it doesn't recognize, so on load error we retry against the
 *  site's own /favicon.ico instead of showing nothing. */
@Composable
fun FaviconImage(
    faviconUrl: String,
    siteUrl: String,
    modifier: Modifier = Modifier
) {
    val siteDomain = remember(siteUrl) {
        // siteUrl is a template like "https://host/path?q={query}" — {} isn't valid URI
        // syntax, so java.net.URI would throw. Pull the host out with a regex instead.
        Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#]+)").find(siteUrl)?.groupValues?.get(1)
    }
    var model by remember(faviconUrl) { mutableStateOf<Any?>(faviconUrl) }
    AsyncImage(
        model = model,
        contentDescription = null,
        onState = { state ->
            val fallback = siteDomain?.let { "https://$it/favicon.ico" }
            if (state is AsyncImagePainter.State.Error && fallback != null && model != fallback) {
                model = fallback
            }
        },
        modifier = modifier
    )
}
