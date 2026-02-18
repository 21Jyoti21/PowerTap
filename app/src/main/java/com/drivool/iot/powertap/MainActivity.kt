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
class MainActivity : AppCompatActivity() {

    lateinit var welcomeTv: TextView
    lateinit var scanResultCard: CardView
    lateinit var scanResultTv: TextView
    lateinit var scanBtn: Button
    lateinit var chargerListLayout: LinearLayout
    lateinit var addChargerFab: FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
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
        val name = prefs.getString("name", "User")
        welcomeTv.text = "Welcome, $name"

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
//            scanResultTv.text="Scanned Results: ${result.contents}"
//            scanResultTv.text = result.contents
//            scanResultCard.visibility = View.VISIBLE
//            addChargerItem(result.contents)
            val raw = result.contents.trim()
            Toast.makeText(this, raw, Toast.LENGTH_LONG).show()
            try {
                val chargerId: String
                val chargerName: String

                if (raw.startsWith("http")) {

                    val decoded = java.net.URLDecoder.decode(raw, "UTF-8")

                    // everything after ?
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
                    // JSON QR
                    val json = JSONObject(raw)
                    chargerId = json.getString("id")
                    chargerName = json.getString("name")

                } else {
                    // Plain ID
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

        chargerCount++

        val card = CardView(this)
        card.radius = 24f
        card.cardElevation = 8f
        card.setCardBackgroundColor(Color.parseColor("#F3F0FF"))


        card.setUseCompatPadding(true)
        card.isClickable = true
        card.isFocusable = true
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 0, 0, 28)
        card.layoutParams = cardParams

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(36, 32, 36, 32)

        val title = TextView(this)
//        title.text = "Charger $chargerCount"
        title.text = chargerName
        title.textSize = 16f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(
            Color.parseColor("#1F1F1F") // dark blackish
        )

        val valueBox = TextView(this)
        valueBox.text = "ID: $chargerId"
//        valueBox.text = chargerId
        valueBox.textSize = 14f
        valueBox.setPadding(0, 12, 0, 0)
        valueBox.setTextColor(
            Color.parseColor("#444444") // dark gray
        )

        container.addView(title)
        container.addView(valueBox)
        card.addView(container)
        chargerListLayout.addView(card)

        card.setOnClickListener {
            openChargerPage(chargerId)
        }
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

        //val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
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
}