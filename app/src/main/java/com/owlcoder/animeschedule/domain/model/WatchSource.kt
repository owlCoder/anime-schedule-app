package com.owlcoder.animeschedule.domain.model

data class WatchSource(
    val id: Int = 0,
    val name: String,
    val urlTemplate: String,
    val faviconUrl: String?,
    val sortOrder: Int = 0,
    val openExternally: Boolean = false
) {
    fun buildUrl(animeTitle: String): String {
        val encoded = java.net.URLEncoder.encode(animeTitle, "UTF-8")
        return urlTemplate.replace("{query}", encoded)
    }
}
