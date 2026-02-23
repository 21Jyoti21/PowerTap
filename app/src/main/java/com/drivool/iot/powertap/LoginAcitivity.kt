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
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
class LoginActivity : AppCompatActivity() {

    lateinit var googleClient: GoogleSignInClient
    lateinit var webView: WebView
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val email = account.email ?: ""
                    val name  = account.displayName ?: "User"

                    val json = JSONObject()
                    json.put("email", email)
                    json.put("name", name)

                    val body = json.toString()
                        .toRequestBody("application/json".toMediaType())

                    val client = OkHttpClient()

                    val request = Request.Builder()
                        .url("http://172.31.124.88:3000/api/v1/auth/google-login")
                        .post(body)
                        .build()

                    client.newCall(request).enqueue(object : Callback {

                        override fun onResponse(call: Call, response: Response) {
                            val resBody = response.body?.string() ?: ""
                            val jsonRes = JSONObject(resBody)

                            val token = jsonRes.getString("token")
                            val userId = jsonRes.getString("userId")

                            runOnUiThread {
                                val prefs = getSharedPreferences("app", MODE_PRIVATE)
                                prefs.edit()
                                    .putString("token", token)
                                    .putString("userId", userId)
                                    .putString("name", name)
                                    .putString("email", email)
                                    .putString("phone", "Not Available")
                                    .apply()

                                Log.d("LOGIN_BRIDGE", "TOKEN SAVED = $token")
                                Log.d("LOGIN_BRIDGE", "USERID SAVED = $userId")

                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                webView.evaluateJavascript(
                                    "alert('Server error during Google login')",
                                    null
                                )
                            }
                        }
                    })
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
        fun onLoginSuccess(token: String,userId:String, name: String,email: String,phone:String) {
            Log.d("LOGIN_BRIDGE", "TOKEN = $token")
            Log.d("LOGIN_BRIDGE", "USERID = $userId")
            Log.d("LOGIN_BRIDGE", "NAME = $name")
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            prefs.edit()
                .putString("token", token)
                .putString("userId", userId)
                .putString("name", name)
                .putString("email", email)
                .putString("phone", phone)
                .apply()
            Log.d("LOGIN", "SAVED USER ID = $userId")
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