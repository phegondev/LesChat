package com.dennisiluma.leschat.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.databinding.adapters.ToolbarBindingAdapter
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.util.AppUtil
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashScreen : AppCompatActivity() {
    private var firebaseAuth: FirebaseAuth? = null
    private lateinit var appUtil: AppUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        firebaseAuth = FirebaseAuth.getInstance()
        appUtil = AppUtil()
        this.supportActionBar?.hide();

        Handler(Looper.getMainLooper()).postDelayed({


            if (firebaseAuth!!.currentUser == null) {

                startActivity(Intent(this, MainActivity::class.java))
                finish()

            } else if (collectSignedInSharedPreferences() == "registeredProfileSuccessfully") {
                startActivity(Intent(this, DashBoard::class.java))
                finish()

            } else {

                /*Every device has its own unique fcm token, here we are writting it*/
//                FirebaseInstanceId.getInstance().instanceId
//                    .addOnCompleteListener(OnCompleteListener {
//                        if (it.isSuccessful) {
//                            val token = it.result?.token
//                            val databaseReference =
//                                FirebaseDatabase.getInstance().getReference("Users")
//                                    .child(appUtil.getUID()!!)
//
//                            val map: MutableMap<String, Any> = HashMap()
//                            map["token"] = token!!
//                            databaseReference.updateChildren(map)
//                        }
//                    })
//                startActivity(Intent(this, DashBoard::class.java))
//                finish()
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