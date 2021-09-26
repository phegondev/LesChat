package com.dennisiluma.leschat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.dennisiluma.leschat.model.UserModel
import com.dennisiluma.leschat.repository.AppRepo

class UserInfoViewModel : ViewModel() {

    private var appRepo = AppRepo.StaticFunction.getInstance()

    fun getSingleUserInfoDetails(userId: String): LiveData<UserModel> {
        return appRepo.getSingleUserInfoDetails(userId)
    }
}