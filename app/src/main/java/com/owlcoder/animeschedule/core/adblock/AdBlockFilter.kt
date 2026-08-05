package com.owlcoder.animeschedule.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Offline ad/tracker filter for the in-app WebView.
 *
 * Host rules cover normal third-party ad requests, while URL fragments catch script and video-ad
 * endpoints such as VAST/prebid/interstitial URLs. The filter intentionally stays conservative for
 * top-level navigation so legitimate watch-server changes keep working.
 */
class AdBlockFilter private constructor(
    private val blockedHosts: Set<String>,
    private val blockedUrlFragments: Set<String>,
) {

    fun shouldBlock(request: WebResourceRequest): Boolean = shouldBlock(request.url)

    fun shouldBlock(uri: Uri): Boolean {
        val host = uri.host?.lowercase().orEmpty()
        if (host.isNotEmpty() && matchesBlockedHost(host)) return true

        val normalizedUrl = uri.toString().lowercase()
        return blockedUrlFragments.any(normalizedUrl::contains) ||
            BUILT_IN_AD_URL_FRAGMENTS.any(normalizedUrl::contains)
    }

    /**
     * Blocks only clearly advertising-shaped automatic cross-origin main-frame redirects.
     * User-initiated links remain allowed because watch providers often change video hosts.
     */
    fun shouldBlockAutomaticNavigation(
        currentUrl: String?,
        target: Uri,
        hasUserGesture: Boolean,
    ): Boolean {
        if (shouldBlock(target)) return true
        if (hasUserGesture) return false

        val currentHost = currentUrl
            ?.let(Uri::parse)
            ?.host
            ?.lowercase()
            ?: return false
        val targetHost = target.host?.lowercase() ?: return false
        if (sameSite(currentHost, targetHost)) return false

        val normalizedTarget = target.toString().lowercase()
        return POPUP_NAVIGATION_FRAGMENTS.any(normalizedTarget::contains)
    }

    private fun matchesBlockedHost(host: String): Boolean =
        host in blockedHosts || blockedHosts.any { host.endsWith(".$it") }

    private fun sameSite(first: String, second: String): Boolean =
        first == second || first.endsWith(".$second") || second.endsWith(".$first")

    companion object {
        @Volatile
        private var instance: AdBlockFilter? = null

        private val BUILT_IN_AD_URL_FRAGMENTS = setOf(
            "adsbygoogle.js",
            "/prebid",
            "prebid.js",
            "/vast?",
            "/vast/",
            "vast.xml",
            "delivery/afr.php",
            "delivery/ajs.php",
            "/interstitial/",
            "popunder",
            "onclickads",
            "onclickmax",
            "propellerclick",
        )

        private val POPUP_NAVIGATION_FRAGMENTS = setOf(
            "popunder",
            "popup",
            "interstitial",
            "onclick",
            "zoneid=",
            "campaignid=",
            "clickid=",
            "click_id=",
            "propellerclick",
        )

        fun getOrLoad(context: Context): AdBlockFilter {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }

                val entries = runCatching {
                    context.assets.open("adblock_hosts.txt").use { stream ->
                        BufferedReader(InputStreamReader(stream)).readLines()
                            .map { it.substringBefore('#').trim().lowercase() }
                            .filter { it.isNotEmpty() }
                    }
                }.getOrDefault(emptyList())

                val hosts = entries
                    .filterNot { '/' in it || it.endsWith(".js") }
                    .map { it.removePrefix("www.") }
                    .toSet()
                val fragments = entries
                    .filter { '/' in it || it.endsWith(".js") }
                    .toSet()

                return AdBlockFilter(
                    blockedHosts = hosts,
                    blockedUrlFragments = fragments,
                ).also { instance = it }
            }
        }
    }
}
