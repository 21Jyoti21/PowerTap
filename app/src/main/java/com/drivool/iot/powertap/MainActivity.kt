package com.drivool.iot.powertap

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    lateinit var welcomeTv: TextView
    lateinit var scanResultCard: CardView
    lateinit var scanResultTv: TextView
    lateinit var scanBtn: Button
    lateinit var chargerListLayout: LinearLayout
    lateinit var addChargerFab: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TEST_LOG", "ANDROID LOG WORKING")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = getColor(R.color.purple_status)

        setContentView(R.layout.activity_main)

        welcomeTv = findViewById(R.id.welcomeTv)
        scanResultCard = findViewById(R.id.scanResultCard)
        scanResultTv=findViewById(R.id.scanResultTv)
        scanBtn=findViewById(R.id.scanBtn)
        chargerListLayout = findViewById(R.id.chargerList)
        addChargerFab = findViewById(R.id.addChargerFab)

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
        val uid = prefs.getString("userId", null)
        Log.d("MAIN_ACTIVITY", "USER ID FROM PREFS = $uid")
        val name = prefs.getString("name", "User")
        welcomeTv.text = "Welcome, $name"

        loadChargers()
        scanBtn.setOnClickListener {
            val options = ScanOptions()
            options.setPrompt("Scan any QR CODE")
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setCaptureActivity(CaptureActivity::class.java)
            barcodeLauncher.launch(options)

        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        addChargerFab.setOnClickListener {
            showManualEntryDialog()
        }
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
    private val barcodeLauncher=registerForActivityResult(ScanContract()){
        result->
        if(result.contents!=null){
            val raw = result.contents.trim()
            Toast.makeText(this, raw, Toast.LENGTH_LONG).show()
            try {
                val chargerId: String
                val chargerName: String

                if (raw.startsWith("http")) {

                    val decoded = java.net.URLDecoder.decode(raw, "UTF-8")
                    val params = decoded.substringAfter("?")

                    if (params.contains("&")) {
                        val parts = params.split("&")

                        chargerId = parts[0].substringAfter("id=")
                        chargerName = parts[1].substringAfter("name=")

                    } else {
                        chargerId = params.substringAfter("id=")
                        chargerName = "PowerTap Charger"
                    }
                } else if (raw.startsWith("{")) {
                    //JSON QR
                    val json = JSONObject(raw)
                    chargerId = json.getString("id")
                    chargerName = json.getString("name")

                } else {
                    //Plain ID
                    chargerId = raw
                    chargerName = "PowerTap Charger"
                }

                addChargerItem(chargerId, chargerName)

            } catch (e: Exception) {
                Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show()
            }
        }else{
            Toast.makeText(this,"Cancelled", Toast.LENGTH_SHORT).show()

        }

    }
    private var chargerCount = 0

    private fun addChargerItem(chargerId: String,chargerName: String) {
        saveChargerToServer(chargerId, chargerName)

//        chargerCount++
//
//        val card = CardView(this)
//        card.radius = 24f
//        card.cardElevation = 8f
//        card.setCardBackgroundColor(Color.parseColor("#F3F0FF"))
//
//
//        card.setUseCompatPadding(true)
//        card.isClickable = true
//        card.isFocusable = true
//        val cardParams = LinearLayout.LayoutParams(
//            LinearLayout.LayoutParams.MATCH_PARENT,
//            LinearLayout.LayoutParams.WRAP_CONTENT
//        )
//        cardParams.setMargins(0, 0, 0, 28)
//        card.layoutParams = cardParams
//
//        val container = LinearLayout(this)
//        container.orientation = LinearLayout.VERTICAL
//        container.setPadding(36, 32, 36, 32)
//
//        val title = TextView(this)
//        title.text = chargerName
//        title.textSize = 16f
//        title.setTypeface(null, Typeface.BOLD)
//        title.setTextColor(
//            Color.parseColor("#1F1F1F") // dark blackish
//        )
//
//        val valueBox = TextView(this)
//        valueBox.text = "ID: $chargerId"
//        valueBox.textSize = 14f
//        valueBox.setPadding(0, 12, 0, 0)
//        valueBox.setTextColor(
//            Color.parseColor("#444444")
//        )
//
//        container.addView(title)
//        container.addView(valueBox)
//        card.addView(container)
//        chargerListLayout.addView(card)
//
//        card.setOnClickListener {
//            openChargerPage(chargerId)
//        }
    }
    private fun openChargerPage(chargerId: String) {
        val intent = Intent(this, ChargerActivity::class.java)
        intent.putExtra("chargerId", chargerId)
        startActivity(intent)
    }
    private fun showManualEntryDialog() {

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 30, 40, 10)

        val idEt = android.widget.EditText(this)
        idEt.hint = "Enter Charger ID"

        val nameEt = android.widget.EditText(this)
        nameEt.hint = "Enter Charger Name"

        layout.addView(nameEt)
        layout.addView(idEt)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Add Charger Manually")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->

                val chargerId = idEt.text.toString().trim()
                val chargerName = nameEt.text.toString().trim()

                if (chargerId.isEmpty() || chargerName.isEmpty()) {
                    Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                } else {
                    addChargerItem(chargerId, chargerName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            .setTextColor(Color.parseColor("#5D4037"))

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(Color.parseColor("#5D4037"))
    }
    fun saveChargerToServer(chargerId: String, chargerName: String) {

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
//        val userId = prefs.getString("userId", null)

//        Log.d("CHARGER_SAVE", "userId=$userId  chargerId=$chargerId")

        val json = JSONObject()
        json.put("chargerId", chargerId)
        json.put("name", chargerName)

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val token = prefs.getString("token", null)

        val request = Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/chargers/add")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CHARGER_SAVE", " NETWORK FAILED ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body!!.string()

                runOnUiThread {

                    val json = JSONObject(res)
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", "Charger already exists")

                    runOnUiThread {

                        if (success) {
                            addChargerItemUI(chargerId, chargerName)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }
            }
        })
    }
    fun loadChargers() {

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
//        val userId = prefs.getString("userId", null)

        val token = prefs.getString("token", null)

        val request = Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/chargers")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("CHARGER_LOAD", "FAILED ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {

                val res = response.body!!.string()

                if (res.trim().startsWith("{")) {
                    val obj = JSONObject(res)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            obj.optString("msg", "Auth error"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return
                }

                val arr = org.json.JSONArray(res)
                Log.d("CHARGER_LOAD", "COUNT = ${arr.length()}")
                runOnUiThread {
                    chargerListLayout.removeAllViews()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        addChargerItemUI(
                            obj.getString("chargerId"),
                            obj.getString("name")
                        )
                    }
                }
            }
        })
    }
    fun addChargerItemUI(chargerId: String, chargerName: String) {

        val card = CardView(this)
        card.radius = 24f
        card.cardElevation = 8f
        card.setCardBackgroundColor(Color.parseColor("#F3F0FF"))

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 16, 16, 24)  // left, top, right, bottom
        card.layoutParams = params

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(36, 32, 36, 32)

        val title = TextView(this)
        title.text = chargerName
        title.textSize = 16f
        title.setTypeface(null, Typeface.BOLD)

        val valueBox = TextView(this)
        valueBox.text = "ID: $chargerId"

        container.addView(title)
        container.addView(valueBox)
        card.addView(container)

        chargerListLayout.addView(card)

        card.setOnClickListener {
            openChargerPage(chargerId)
        }
        card.setOnLongClickListener {

            AlertDialog.Builder(this)
                .setTitle("Remove Charger?")
                .setMessage("This will hide the charger from your list.")
                .setPositiveButton("Remove") { _, _ ->
                    removeChargerFromServer(chargerId)
                    chargerListLayout.removeView(card)
                }
                .setNegativeButton("Cancel", null)
                .show()

            true
        }
    }
    fun removeChargerFromServer(chargerId: String) {

        val prefs = getSharedPreferences("app", MODE_PRIVATE)
//        val userId = prefs.getString("userId", null)

        val json = JSONObject()
        json.put("chargerId", chargerId)

        val body = json.toString()
            .toRequestBody("application/json".toMediaType())

        val token = prefs.getString("token", null)

        val request = Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/chargers/remove")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CHARGER_REMOVE", "FAILED ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("CHARGER_REMOVE", "SUCCESS")
            }
        })
    }
}