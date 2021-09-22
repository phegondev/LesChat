package com.dennisiluma.leschat.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.databinding.ActivityDashBoardBinding
import com.dennisiluma.leschat.fragment.ChatFragment
import com.dennisiluma.leschat.fragment.ContactFragment
import com.dennisiluma.leschat.fragment.ProfileFragment

class DashBoard : AppCompatActivity() {
    private lateinit var binding: ActivityDashBoardBinding
    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, ChatFragment())
                .commit()
            binding.bottomChip.setItemSelected(R.id.btnChat)
        }
        binding.bottomChip.setOnItemSelectedListener {
            when(it){
                R.id.btnChat -> {
                    fragment = ChatFragment()
                }
                R.id.btnProfile -> {
                    fragment = ProfileFragment();
                }
                R.id.btnContact -> fragment = ContactFragment()
            }
        }
        fragment.let {  //when fragment comtains something, then replace the fragment holder framelayout in the dashboard xml wit the curresponding fragment
            if (it != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.dashboardContainer,it)
                    .commit()
            }
        }

    }
}