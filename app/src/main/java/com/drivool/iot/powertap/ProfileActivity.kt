package com.drivool.iot.powertap

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ProfileActivity : AppCompatActivity() {
    private val IMAGE_PICK_CODE = 101
    private var isEditMode = false
    private var originalName = ""
    private var originalPhone = ""
    private var originalEmail = ""
    private var isTextChanged = false
    private var isAvatarChanged = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        val prefs = getSharedPreferences("app", MODE_PRIVATE)

        val savedAvatar = prefs.getString("avatar", "")
        if (!savedAvatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(savedAvatar)
                .placeholder(R.drawable.default_avatar)
                .into(findViewById(R.id.profileImg))
        }
        loadProfile()
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
//        val prefs = getSharedPreferences("app", MODE_PRIVATE)


        nameTv.text = prefs.getString("name", "User")
        emailTv.text = prefs.getString("email", "Not Available")
        phoneTv.text = prefs.getString("phone", "Not Available")

        logoutLayout.setOnClickListener {
        prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        val editButton = findViewById<MaterialButton>(R.id.editBtn)
        val editIcon = findViewById<ImageView>(R.id.editProfileImg)
        val profileImg = findViewById<CircleImageView>(R.id.profileImg)

        editIcon.visibility = View.GONE   // hide by default


        editIcon.setOnClickListener {
            if (!isEditMode) return@setOnClickListener

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        profileImg.setOnClickListener {
            if (!isEditMode) return@setOnClickListener

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }
        val phoneEt = findViewById<EditText>(R.id.phoneEt)
        val emailEt = findViewById<EditText>(R.id.emailEt)
        val nameEt = findViewById<EditText>(R.id.nameEt)
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSaveButtonState(editButton, nameEt, phoneEt, emailEt)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        nameEt.addTextChangedListener(watcher)
        phoneEt.addTextChangedListener(watcher)
        emailEt.addTextChangedListener(watcher)
        editButton.setOnClickListener {

            isEditMode = !isEditMode

            if (isEditMode) {
                originalName = nameTv.text.toString()
                originalPhone = phoneTv.text.toString()
                originalEmail = emailTv.text.toString()
                editButton.text = "Save"
                editIcon.visibility = View.VISIBLE

                // show edit boxes
                nameEt.setText(nameTv.text)
                phoneEt.setText(phoneTv.text)
                emailEt.setText(emailTv.text)

                // Hide TextViews
                nameTv.visibility = View.GONE
                phoneTv.visibility = View.GONE
                emailTv.visibility = View.GONE

                // Show EditTexts
                nameEt.visibility = View.VISIBLE
                phoneEt.visibility = View.VISIBLE
                emailEt.visibility = View.VISIBLE
                nameEt.requestFocus()

            } else {
                editButton.setBackgroundTintList(
                    getColorStateList(R.color.brown_light)
                )
                editButton.text = "Edit"

                editIcon.visibility = View.GONE
                val updatedName = nameEt.text.toString().trim()
                val updatedPhone = phoneEt.text.toString().trim()
                val updatedEmail = emailEt.text.toString().trim()

                // 🔥 CALL BACKEND HERE
                if (isTextChanged || isAvatarChanged) {

                    if (isTextChanged) {
                        updateProfileToServer(updatedName, updatedPhone, updatedEmail)
                    }


                }
                // update UI
                nameTv.text = updatedName
                phoneTv.text = updatedPhone
                emailTv.text = updatedEmail

                nameEt.visibility = View.GONE
                phoneEt.visibility = View.GONE
                emailEt.visibility = View.GONE

                nameTv.visibility = View.VISIBLE
                phoneTv.visibility = View.VISIBLE
                emailTv.visibility = View.VISIBLE
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            val imageUri = data?.data
            Log.d("UPLOAD_DEBUG", "Picked URI: $imageUri")
            if (imageUri != null) {
                isAvatarChanged = true
                updateSaveButtonState(
                    findViewById(R.id.editBtn),
                    findViewById(R.id.nameEt),
                    findViewById(R.id.phoneEt),
                    findViewById(R.id.emailEt)
                )
                val profileImg = findViewById<CircleImageView>(R.id.profileImg)
                profileImg.setImageURI(imageUri)
                uploadAvatarToServer(imageUri)
            }
        }
    }
    private fun uploadAvatarToServer(uri: android.net.Uri) {

        Log.d("UPLOAD_DEBUG", "Starting upload...")

        // 1️⃣ Convert URI → Bitmap
        Thread{
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

        // 2️⃣ Compress bitmap
        val stream = ByteArrayOutputStream()
        val maxSize = 800

        val width = bitmap.width
        val height = bitmap.height

        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            // Landscape
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            // Portrait
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        resized.compress(Bitmap.CompressFormat.JPEG, 40, stream)

        val bytes = stream.toByteArray()

        Log.d("UPLOAD_DEBUG", "Compressed size = ${bytes.size}")

        // 3️⃣ Create request body
        val requestBody = okhttp3.RequestBody.create(
            "image/jpeg".toMediaTypeOrNull(),
            bytes
        )

        val body = okhttp3.MultipartBody.Part.createFormData(
            "avatar",
            "avatar.jpg",
            requestBody
        )

        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/user/upload-avatar")
            .addHeader("Authorization", "Bearer $token")
            .post(
                okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addPart(body)
                    .build()
            )
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.e("UPLOAD_DEBUG", "Upload failed: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {

                val res = response.body?.string()

                Log.d("UPLOAD_DEBUG", "Upload success code: ${response.code}")
                Log.d("UPLOAD_DEBUG", "Response: $res")

                val json = org.json.JSONObject(res!!)
                val imageUrl = json.getString("avatar")
                getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .putString("avatar", imageUrl)
                    .apply()
                runOnUiThread {

                    if (isDestroyed || isFinishing) return@runOnUiThread

                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(
                            com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
                        )
                        .placeholder(R.drawable.default_avatar)
                        .into(findViewById(R.id.profileImg))

                    loadProfile()
                    isAvatarChanged = false

                    updateSaveButtonState(
                        findViewById(R.id.editBtn),
                        findViewById(R.id.nameEt),
                        findViewById(R.id.phoneEt),
                        findViewById(R.id.emailEt)
                    )
                }

            }
        })
        }.start()
    }
    private fun loadProfile() {

        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val request = okhttp3.Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/user/me")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {

                val res = response.body?.string()
                if(!response.isSuccessful || res.isNullOrEmpty())return
                val json = org.json.JSONObject(res!!)

                val avatar = json.optString("avatar","")
                val phone = json.optString("phone","")
                val name = json.optString("name","user")

                runOnUiThread {

                    findViewById<TextView>(R.id.phoneTv).text = phone
                    findViewById<TextView>(R.id.nameTv).text = name

                    if (avatar.isNotEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(avatar)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .placeholder(R.drawable.default_avatar)
                            .into(findViewById(R.id.profileImg))
                    }
                }
            }
        })
    }
    private fun updateProfileToServer(
        name: String,
        phone: String,
        email: String
    ) {

        val token = getSharedPreferences("app", MODE_PRIVATE)
            .getString("token", "") ?: ""

        val json = org.json.JSONObject()
        json.put("name", name)
        json.put("phone", phone)
        json.put("email", email)

        val body = okhttp3.RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = okhttp3.Request.Builder()
            .url("http://172.31.124.88:3000/api/v1/user/update-profile")
            .addHeader("Authorization", "Bearer $token")
            .put(body)
            .build()

        val client = okhttp3.OkHttpClient()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Profile update failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {

                runOnUiThread {
                    if (response.isSuccessful) {

                        // ✅ UPDATE ORIGINAL VALUES
                        originalName = findViewById<EditText>(R.id.nameEt).text.toString()
                        originalPhone = findViewById<EditText>(R.id.phoneEt).text.toString()
                        originalEmail = findViewById<EditText>(R.id.emailEt).text.toString()

                        isTextChanged = false
                        isAvatarChanged = false

                        updateSaveButtonState(
                            findViewById(R.id.editBtn),
                            findViewById(R.id.nameEt),
                            findViewById(R.id.phoneEt),
                            findViewById(R.id.emailEt)
                        )

                        Toast.makeText(
                            this@ProfileActivity,
                            "Profile Updated",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Update error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

    }
    private fun updateSaveButtonState(
        editButton: MaterialButton,
        nameEt: EditText,
        phoneEt: EditText,
        emailEt: EditText
    ) {

        val changed =
            isAvatarChanged ||
                    nameEt.text.toString() != originalName ||
                    phoneEt.text.toString() != originalPhone ||
                    emailEt.text.toString() != originalEmail
        isTextChanged = changed
        if (changed) {
        // Dark Brown
        editButton.setBackgroundTintList(
            getColorStateList(R.color.brown_dark) )
        } else {
            editButton.setBackgroundTintList(
                getColorStateList(R.color.brown_light)
            )
        }

    }
}