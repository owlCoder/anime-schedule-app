package com.owlcoder.animeschedule.presentation.screens.watch

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.owlcoder.animeschedule.R
import com.owlcoder.animeschedule.core.adblock.AdBlockFilter
import com.owlcoder.animeschedule.presentation.components.AppLoadingState
import com.owlcoder.animeschedule.presentation.components.GlassChrome
import java.io.ByteArrayInputStream

/** Fullscreen in-app browser for a watch-source link — no address bar/chrome, mirrors
 * visiting the site directly. Back navigates the WebView's own history before popping.
 * Also supports HTML5 `<video>` fullscreen (site's own fullscreen button) via
 * [WebChromeClient.onShowCustomView], which a plain WebView doesn't handle. */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun WatchScreen(
    url: String,
    onBack: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isVideoFullscreen by remember { mutableStateOf(false) }
    var fullscreenChromeClient by remember { mutableStateOf<VideoFullscreenChromeClient?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    val currentWebView by rememberUpdatedState(webView)
    val currentChromeClient by rememberUpdatedState(fullscreenChromeClient)

    // Fallback state preservation for process/config recreation. MainActivity also handles
    // orientation changes directly, so normal rotation keeps the same live WebView instance.
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
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(innerPadding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    val adBlockFilter = AdBlockFilter.getOrLoad(context)
                    WebView(viewContext).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.safeBrowsingEnabled = true

                        isFocusable = true
                        isFocusableInTouchMode = true
                        setOnTouchListener { view, event ->
                            if (event.actionMasked == MotionEvent.ACTION_DOWN && !view.hasFocus()) {
                                view.requestFocus(View.FOCUS_DOWN)
                            }
                            false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageCommitVisible(view: WebView?, url: String?) {
                                super.onPageCommitVisible(view, url)
                                view?.installPopupGuard()
                                view?.requestFocus()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.installPopupGuard()
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                request ?: return true
                                val scheme = request.url.scheme
                                if (scheme != "http" && scheme != "https") return true
                                if (adBlockFilter.shouldBlock(request)) return true
                                if (
                                    request.isForMainFrame &&
                                    adBlockFilter.shouldBlockAutomaticNavigation(
                                        currentUrl = view?.url,
                                        target = request.url,
                                        hasUserGesture = request.hasGesture(),
                                    )
                                ) {
                                    return true
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): WebResourceResponse? {
                                if (request != null && adBlockFilter.shouldBlock(request)) {
                                    return emptyWebResponse()
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        if (activity != null) {
                            webChromeClient = VideoFullscreenChromeClient(
                                activity = activity,
                                onFullscreenChange = { isVideoFullscreen = it },
                            ).also { fullscreenChromeClient = it }
                        }

                        val savedBundle = webViewBundle
                        if (savedBundle != null) {
                            restoreState(savedBundle)
                            isLoading = false
                        } else {
                            loadUrl(url)
                        }
                        requestFocus(View.FOCUS_DOWN)
                    }.also { webView = it }
                },
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppLoadingState(label = stringResource(R.string.common_loading))
                }
            }

            GlassChrome(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            if (isVideoFullscreen) {
                                fullscreenChromeClient?.onHideCustomView()
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.watch_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    DisposableEffect(activity) {
        activity?.let { setSystemBarsVisible(it, visible = true) }
        onDispose {
            currentChromeClient?.onHideCustomView()
            currentWebView?.let { activeWebView ->
                webViewBundle = Bundle().also { activeWebView.saveState(it) }
                activeWebView.releaseResources()
            }
            activity?.let { setSystemBarsVisible(it, visible = true) }
        }
    }
}

private fun WebView.installPopupGuard() {
    evaluateJavascript(POPUP_GUARD_SCRIPT, null)
}

private fun WebView.releaseResources() {
    stopLoading()
    webChromeClient = null
    webViewClient = WebViewClient()
    setOnTouchListener(null)
    removeAllViews()
    destroy()
}

private fun emptyWebResponse(): WebResourceResponse = WebResourceResponse(
    "text/plain",
    "UTF-8",
    ByteArrayInputStream(ByteArray(0)),
)

/** Hosts a site's HTML5 video fullscreen view as an overlay on the activity's decor view. */
private class VideoFullscreenChromeClient(
    private val activity: Activity,
    private val onFullscreenChange: (Boolean) -> Unit,
) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    /** Defense-in-depth if a provider requests a secondary WebView despite window support being off. */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean = false

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
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
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

private const val POPUP_GUARD_SCRIPT = """
    (function() {
        if (window.__animeSchedulePopupGuardInstalled) return;
        window.__animeSchedulePopupGuardInstalled = true;

        try {
            window.open = function() { return null; };
        } catch (_) {}

        function sanitizeTarget(node) {
            if (!node || !node.getAttribute) return;
            var tag = (node.tagName || '').toUpperCase();
            if (tag !== 'A' && tag !== 'AREA' && tag !== 'FORM') return;

            var target = (node.getAttribute('target') || '').toLowerCase();
            if (target === '_blank' || target === '_new') node.removeAttribute('target');
            if (tag === 'A' || tag === 'AREA') {
                node.setAttribute('rel', 'noopener noreferrer');
            }
        }

        function sanitize(root) {
            if (!root) return;
            sanitizeTarget(root);
            if (!root.querySelectorAll) return;
            root.querySelectorAll('a[target], area[target], form[target]').forEach(sanitizeTarget);
        }

        sanitize(document);
        if (document.documentElement) {
            new MutationObserver(function(records) {
                records.forEach(function(record) {
                    if (record.type === 'attributes') {
                        sanitizeTarget(record.target);
                    }
                    record.addedNodes.forEach(function(node) {
                        if (node.nodeType === 1) sanitize(node);
                    });
                });
            }).observe(document.documentElement, {
                subtree: true,
                childList: true,
                attributes: true,
                attributeFilter: ['target']
            });
        }

        function neutralizeEventTarget(event) {
            var node = event.target;
            while (node && node !== document && node.tagName !== 'A' && node.tagName !== 'AREA' && node.tagName !== 'FORM') {
                node = node.parentNode;
            }
            sanitizeTarget(node);
        }

        document.addEventListener('pointerdown', neutralizeEventTarget, true);
        document.addEventListener('touchstart', neutralizeEventTarget, true);
        document.addEventListener('click', neutralizeEventTarget, true);
    })();
"""
