package com.dennisiluma.leschat.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.dennisiluma.leschat.R
import com.theartofdev.edmodo.cropper.CropImage.activity

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            if (collectSignedInSharedPreferences() == "registeredProfileSuccessfully") {
                startActivity(Intent(this, DashBoard::class.java))
                finish()
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }, 3000)

    }

    fun collectSignedInSharedPreferences(): String? { //comming from GetUserData fragment line 141
        val sharedPref =
            this.getSharedPreferences("registeredProfileSuccessfully", Context.MODE_PRIVATE)
        return sharedPref.getString(
            "registeredProfileSuccessfully",
            "registeredProfileSuccessfully"
        )
    }
}