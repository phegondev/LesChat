package com.dennisiluma.leschat.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.*
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.databinding.ActivityUserInfoBinding
import com.dennisiluma.leschat.util.AppUtil
import com.dennisiluma.leschat.viewmodel.ProfileViewModel
import com.dennisiluma.leschat.viewmodel.UserInfoViewModel

class UserInfoActivity : AppCompatActivity() {
    private lateinit var userInfoBinding: ActivityUserInfoBinding

    private lateinit var userInfoViewModel: UserInfoViewModel
    private var userId: String? = null
    private lateinit var appUtil: AppUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userInfoBinding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(userInfoBinding.root)
        supportActionBar?.title = "User Info"

        userInfoViewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
            .create(UserInfoViewModel::class.java)

        userId = intent.getStringExtra("userId")

        getSingleUserInfoDetails(userId)
    }

    private fun getSingleUserInfoDetails(userId: String?) {
        if (userId != null) {
            userInfoViewModel.getSingleUserInfoDetails(userId).observe(this, { userModel ->
                userInfoBinding.userModel = userModel
                if (userModel.name.contains(" ")) {
                    val split = userModel.name.split(" ")
                    userInfoBinding.txtProfileFName.text = split[0]
                    userInfoBinding.txtProfileLName.text = split[1]
                }
            })
        }
    }
    override fun onPause() {
        super.onPause()
        appUtil.updateOnlineStatus("offline")
    }

    override fun onResume() {
        super.onResume()
        appUtil.updateOnlineStatus("online")
    }
}