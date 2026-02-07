package com.drivool.iot.powertap

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadUrl("file:///android_asset/login.html")
    }

    inner class WebAppInterface(private val activity: LoginActivity) {

        @JavascriptInterface
        fun onLoginSuccess(username: String) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra("username", username)
            activity.startActivity(intent)
            activity.finish()
        }
    }
}