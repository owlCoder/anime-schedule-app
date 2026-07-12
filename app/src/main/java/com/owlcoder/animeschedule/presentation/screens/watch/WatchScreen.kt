package com.owlcoder.animeschedule.presentation.screens.watch

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.owlcoder.animeschedule.core.adblock.AdBlockFilter
import java.io.ByteArrayInputStream

/** Fullscreen in-app browser for a watch-source link — no address bar/chrome, mirrors
 *  visiting the site directly. Back navigates the WebView's own history before popping.
 *  Also supports HTML5 `<video>` fullscreen (site's own fullscreen button) via
 *  [WebChromeClient.onShowCustomView], which a plain WebView doesn't handle. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WatchScreen(
    url: String,
    onBack: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isVideoFullscreen by remember { mutableStateOf(false) }
    var fullscreenChromeClient by remember { mutableStateOf<VideoFullscreenChromeClient?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity

    // Preserves the WebView's own in-site navigation (e.g. a picked video server/episode)
    // across a config change (rotation) recreating this Activity — otherwise the factory
    // below would spin up a brand-new WebView and reload the original watch-source `url`,
    // dropping the user back on the source's search/landing page mid-playback.
    var webViewBundle by rememberSaveable { mutableStateOf<Bundle?>(null) }

    BackHandler {
        val chromeClient = fullscreenChromeClient
        val wv = webView
        when {
            isVideoFullscreen -> chromeClient?.onHideCustomView()
            wv != null && wv.canGoBack() -> wv.goBack()
            else -> onBack()
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    val adBlockFilter = AdBlockFilter.getOrLoad(context)
                    WebView(viewContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            // Shady streaming sites redirect to non-http(s) schemes
                            // (intent://, market://, etc.) as disguised ads/malware links.
                            // A plain WebView can't open those anyway (ERR_UNKNOWN_URL_SCHEME) —
                            // swallow the navigation instead of showing an error page.
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val scheme = request?.url?.scheme
                                if (scheme != "http" && scheme != "https") return true
                                return super.shouldOverrideUrlLoading(view, request)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (request != null && adBlockFilter.shouldBlock(request)) {
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        if (activity != null) {
                            webChromeClient = VideoFullscreenChromeClient(
                                activity = activity,
                                onFullscreenChange = { isVideoFullscreen = it }
                            ).also { fullscreenChromeClient = it }
                        }
                        val savedBundle = webViewBundle
                        if (savedBundle != null) {
                            restoreState(savedBundle)
                            isLoading = false
                        } else {
                            loadUrl(url)
                        }
                    }.also { webView = it }
                }
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { wv -> webViewBundle = Bundle().also { wv.saveState(it) } }
        }
    }
}

/** Hosts a site's HTML5 video fullscreen view as an overlay on the activity's decor view,
 *  since a plain WebView has nowhere to put it. [onFullscreenChange] lets the composable
 *  know so it can route back-press to exiting fullscreen instead of WebView navigation. */
private class VideoFullscreenChromeClient(
    private val activity: Activity,
    private val onFullscreenChange: (Boolean) -> Unit
) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null || view == null) {
            callback?.onCustomViewHidden()
            return
        }
        val decor = activity.window.decorView as ViewGroup
        customView = view
        customViewCallback = callback
        decor.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setSystemBarsVisible(activity, visible = false)
        onFullscreenChange(true)
    }

    override fun onHideCustomView() {
        val decor = activity.window.decorView as ViewGroup
        customView?.let { decor.removeView(it) }
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        setSystemBarsVisible(activity, visible = true)
        onFullscreenChange(false)
    }
}

private fun setSystemBarsVisible(activity: Activity, visible: Boolean) {
    val windowInsetsController =
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    if (visible) {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    } else {
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
