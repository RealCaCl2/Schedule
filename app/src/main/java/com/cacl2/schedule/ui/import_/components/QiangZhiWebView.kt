package com.cacl2.schedule.ui.import_.components

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QiangZhiWebView(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    onPageFinished: (String) -> Unit,
    modifier: Modifier = Modifier,
    onWebViewDisposed: (WebView) -> Unit = {}
) {
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.let { webView ->
                webView.stopLoading()
                webView.destroy()
                onWebViewDisposed(webView)
            }
            webViewInstance = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    allowFileAccess = false
                    allowContentAccess = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = true
                    }
                    userAgentString = settings.userAgentString.replace("; wv", "")
                }

                val webView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(webView, false)
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        pageUrl?.let { onPageFinished(it) }
                    }
                }

                webChromeClient = WebChromeClient()

                if (url.isNotBlank() && url != "about:blank") {
                    loadUrl(url)
                    lastLoadedUrl = url
                }

                webViewInstance = this
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (url.isBlank() || url == "about:blank") return@AndroidView
            if (url != lastLoadedUrl) {
                webView.loadUrl(url)
                lastLoadedUrl = url
            }
        }
    )
}
