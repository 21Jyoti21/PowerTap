package com.drivool.iot.powertap

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity

class ChargerActivity : AppCompatActivity() {

    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        val chargerId = intent.getStringExtra("chargerId")

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.cacheMode = WebSettings.LOAD_DEFAULT

        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/charger.html?dev=$chargerId")
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

    }

    inner class AndroidBridge {

        @JavascriptInterface
        fun goBackToMain() {
            runOnUiThread {
                finish()
            }
        }
    }
}
