package com.ucb.capstone.farmnook.utils

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

fun loadMapInWebView(webView: WebView, pickup: String, drop: String) {
    val encodedPickup = pickup.replace(" ", "")
    val encodedDrop = drop.replace(" ", "")
    val mapUrl = "https://farmnook-web.vercel.app/map-viewer?pickup=$encodedPickup&drop=$encodedDrop"

    webView.settings.javaScriptEnabled = true
    webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
    webView.settings.domStorageEnabled = true
    webView.webViewClient = WebViewClient()
    webView.loadUrl(mapUrl)
}