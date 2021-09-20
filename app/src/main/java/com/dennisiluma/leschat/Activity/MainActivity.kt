package com.dennisiluma.leschat.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dennisiluma.leschat.Fragment.GetUserNumber
import com.dennisiluma.leschat.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
            .add(R.id.main_container, GetUserNumber())
            .commit()
    }
}