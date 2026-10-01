package com.owlcoder.animeschedule.domain.model

import java.net.URI
import java.net.URLEncoder

private const val QUERY_PLACEHOLDER = "{query}"

data class WatchSource(
    val id: Int = 0,
    val name: String,
    val urlTemplate: String,
    val faviconUrl: String?,
    val sortOrder: Int = 0,
    val openExternally: Boolean = false
) {
    fun buildUrl(animeTitle: String): String {
        val encoded = URLEncoder.encode(animeTitle, "UTF-8")
        return urlTemplate.replace(QUERY_PLACEHOLDER, encoded)
    }
}

/** A usable watch-source template is an http(s) URL with a host and the `{query}` placeholder. */
fun isValidWatchSourceTemplate(template: String): Boolean =
    template.contains(QUERY_PLACEHOLDER) && isWebUrl(template.replace(QUERY_PLACEHOLDER, "q"))

/**
 * True for absolute http(s) URLs with a host. Watch sources are user-defined, so anything that
 * is opened (in the in-app browser or another app) is checked against this first.
 */
fun isWebUrl(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    return (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) &&
        !uri.host.isNullOrBlank()
}
