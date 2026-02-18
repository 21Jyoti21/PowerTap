package com.drivool.iot.powertap

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    lateinit var googleClient: GoogleSignInClient
    lateinit var webView: WebView
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val token = account.idToken ?: ""

                    val name = account.displayName ?: "User"
                    val email = account.email ?: "Not Available"

                    runOnUiThread {
                        val prefs = getSharedPreferences("app", MODE_PRIVATE)
                        prefs.edit()
                            .putString("token", token)
                            .putString("name", name)
                            .putString("email", email)
                            .putString("phone", "Not Available")
                            .apply()


                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        webView.evaluateJavascript(
                            "alert('Google login failed')",
                            null
                        )
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        if (prefs.getString("token", null) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.loadUrl("file:///android_asset/login.html")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)
    }
    inner class WebAppInterface() {

        @JavascriptInterface
        fun onLoginSuccess(token: String, name: String,email: String,phone:String) {
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            prefs.edit()
                .putString("token", token)
                .putString("name", name)
                .putString("email", email)
                .putString("phone", phone)
                .apply()

            val intent = Intent(this@LoginActivity, MainActivity::class.java)
//            intent.putExtra("username", username)
            startActivity(intent)
            finish()
        }
        @android.webkit.JavascriptInterface
        fun startGoogleLogin() {
            googleClient.signOut().addOnCompleteListener {
                val signInIntent = googleClient.signInIntent
                googleLauncher.launch(signInIntent)
            }
        }
    }
}