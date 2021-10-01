package com.dennisiluma.leschat.activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dennisiluma.leschat.fragment.GetUserNumber
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.fragment.GetUserData

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (collectIncompleteSignedInSharedPreferences() == "registeredButNotFullyComplete"){
            supportFragmentManager.beginTransaction()
                .add(R.id.main_container, GetUserData())
                .commit()
        }else{
            supportFragmentManager.beginTransaction()
                .add(R.id.main_container, GetUserNumber())
                .commit()

        }
    }

    fun collectIncompleteSignedInSharedPreferences(): String? { //comming from verify_number fragment line 71
        val sharedPref = this.getSharedPreferences("registeredButNotFullyComplete", Context.MODE_PRIVATE)

        return  sharedPref.getString("registeredButNotFullyComplete", "registeredButNotFullyComplete")

    }
}