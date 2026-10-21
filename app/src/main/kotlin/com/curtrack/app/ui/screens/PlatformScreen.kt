package com.curtrack.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.curtrack.app.LocalNav
import com.curtrack.app.ui.components.*
import com.curtrack.app.ui.theme.C

/** The CurTrack portal on SecureGPSTracking (pid 189). Credentials go straight to the platform; nothing passes through us. */
const val PLATFORM_URL = "https://v3.securegpstracking.com/html5/jsp/common/login.jsp?pid=189"

/**
 * Live-platform bridge (v1.1): the platform's own HTML5 client inside the app, so real vehicles are
 * available before the API integration (handoff README §0, prerequisites #1/#2) lands.
 * Cookies persist, so "Auto Login" on the platform keeps the user signed in between launches.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformScreen() {
    val nav = LocalNav.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var title by remember { mutableStateOf("Connecting…") }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler { if (canGoBack) webView?.goBack() else nav.popBackStack() }

    Column(Modifier.fillMaxSize().background(C.bg).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(Icons.back, "Back to app", ghost = true) { nav.popBackStack() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Kicker("Live platform · portal 189"); H2(title) }
            IconButton(Icons.refresh, "Reload") { webView?.reload() }
        }
        if (progress in 0.01f..0.99f) ProgressBar(progress, C.teal) else Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.userAgentString = settings.userAgentString + " CurTrackApp/1.1"
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val host = request.url.host ?: return false
                            // Stay inside the platform; anything else (phone links, external sites) goes to the system.
                            return if (host.endsWith("securegpstracking.com")) false
                            else { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)) }; true }
                        }
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) { canGoBack = view.canGoBack() }
                        override fun onPageFinished(view: WebView, url: String?) {
                            canGoBack = view.canGoBack(); CookieManager.getInstance().flush()
                            title = view.title?.takeIf { it.isNotBlank() } ?: "CurTrack"
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress / 100f }
                        override fun onReceivedTitle(view: WebView, t: String?) { title = t?.takeIf { it.isNotBlank() } ?: "CurTrack" }
                    }
                    loadUrl(PLATFORM_URL)
                    webView = this
                }
            },
        )
    }
}
