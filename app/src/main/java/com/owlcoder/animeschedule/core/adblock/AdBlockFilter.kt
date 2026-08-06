package com.owlcoder.animeschedule.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest

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

    /**
     * Checks the exact host and each parent suffix with O(label count) hash lookups instead of
     * scanning the complete block list for every WebView resource request.
     */
    private fun matchesBlockedHost(host: String): Boolean {
        var candidate = host.removeSuffix(".")
        while (candidate.isNotEmpty()) {
            if (candidate in blockedHosts) return true
            val nextLabel = candidate.indexOf('.')
            if (nextLabel < 0) return false
            candidate = candidate.substring(nextLabel + 1)
        }
        return false
    }

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
                    context.assets.open("adblock_hosts.txt").bufferedReader().useLines { lines ->
                        lines.map { line -> line.substringBefore('#').trim().lowercase() }
                            .filter { line -> line.isNotEmpty() }
                            .toList()
                    }
                }.getOrDefault(emptyList())

                val hosts = HashSet<String>(entries.size)
                val fragments = HashSet<String>()
                entries.forEach { entry ->
                    if ('/' in entry || entry.endsWith(".js")) {
                        fragments += entry
                    } else {
                        hosts += entry.removePrefix("www.")
                    }
                }

                return AdBlockFilter(
                    blockedHosts = hosts,
                    blockedUrlFragments = fragments,
                ).also { instance = it }
            }
        }
    }
}
