package com.drivool.iot.powertap
import com.bumptech.glide.Glide

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val gifBg = findViewById<ImageView>(R.id.gifBg)

        Glide.with(this)
            .asGif()
            .load(R.drawable.back)
            .placeholder(R.drawable.back2)
            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
            .into(gifBg)

        val nameTv = findViewById<TextView>(R.id.nameTv)
        val emailTv = findViewById<TextView>(R.id.emailTv)
        val phoneTv = findViewById<TextView>(R.id.phoneTv)
        val logoutLayout = findViewById<LinearLayout>(R.id.logoutLayout)
        val prefs = getSharedPreferences("app", MODE_PRIVATE)


        nameTv.text = prefs.getString("name", "User")
        emailTv.text = prefs.getString("email", "Not Available")
        phoneTv.text = prefs.getString("phone", "Not Available")

        logoutLayout.setOnClickListener {
        prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}