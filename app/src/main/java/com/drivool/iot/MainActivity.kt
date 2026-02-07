package com.drivool.iot.powertap

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {

    lateinit var welcomeTv: TextView
    lateinit var scanResultCard: androidx.cardview.widget.CardView
    lateinit var scanResultTv: TextView
    lateinit var scanBtn: Button
    lateinit var chargerListLayout: android.widget.LinearLayout

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

        val username = intent.getStringExtra("username") ?: "User"
        welcomeTv.text = "Welcome, $username"

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
    }
    private val barcodeLauncher=registerForActivityResult(ScanContract()){
        result->
        if(result.contents!=null){
//            scanResultTv.text="Scanned Results: ${result.contents}"
//            scanResultTv.text = result.contents
//            scanResultCard.visibility = View.VISIBLE
            addChargerItem(result.contents)

        }else{
            Toast.makeText(this,"Cancelled",Toast.LENGTH_SHORT).show()

        }

    }
    private var chargerCount = 0

    private fun addChargerItem(chargerId: String) {

        chargerCount++

        val card = androidx.cardview.widget.CardView(this)
        card.radius = 24f
        card.cardElevation = 8f
        card.setCardBackgroundColor(android.graphics.Color.parseColor("#F3F0FF"))


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
        title.text = "Charger $chargerCount"
        title.textSize = 16f
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setTextColor(
            android.graphics.Color.parseColor("#1F1F1F") // dark blackish
        )

        val valueBox = TextView(this)
        valueBox.text = chargerId
        valueBox.textSize = 14f
        valueBox.setPadding(0, 12, 0, 0)
        valueBox.setTextColor(
            android.graphics.Color.parseColor("#444444") // dark gray
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
}