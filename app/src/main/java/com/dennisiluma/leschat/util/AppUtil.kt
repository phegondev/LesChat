package com.dennisiluma.leschat.util

import com.google.firebase.auth.FirebaseAuth

class AppUtil {

    fun getUID(): String? {
        val firebaseAuth = FirebaseAuth.getInstance()
        return firebaseAuth.uid
    }
}