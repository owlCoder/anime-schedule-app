package com.owlcoder.animeschedule.core.adblock

import android.content.Context
import android.webkit.WebResourceRequest
import java.io.BufferedReader
import java.io.InputStreamReader

/** Offline host-based ad/tracker blocklist for the in-app WebView (see [WatchScreen]). Loaded
 *  once from a bundled asset — no network calls, no auto-updating filter subscriptions. */
class AdBlockFilter private constructor(private val blockedHosts: Set<String>) {

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host ?: return false
        // Exact match or any subdomain of a blocked host (e.g. "ads.doubleclick.net"
        // matches the "doubleclick.net" list entry).
        return host in blockedHosts || blockedHosts.any { host.endsWith(".$it") }
    }

    companion object {
        @Volatile private var instance: AdBlockFilter? = null

        fun getOrLoad(context: Context): AdBlockFilter {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val hosts = runCatching {
                    context.assets.open("adblock_hosts.txt").use { stream ->
                        BufferedReader(InputStreamReader(stream)).readLines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .toSet()
                    }
                }.getOrDefault(emptySet())
                return AdBlockFilter(hosts).also { instance = it }
            }
        }
    }
}
