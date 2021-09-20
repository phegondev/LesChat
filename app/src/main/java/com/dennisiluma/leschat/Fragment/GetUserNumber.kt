package com.dennisiluma.leschat.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.UserModel
import com.dennisiluma.leschat.databinding.FragmentGetUserNumberBinding
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import java.util.concurrent.TimeUnit

class GetUserNumber : Fragment() {
    private var number: String? = null
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private var code: String? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var databaseReference: DatabaseReference? = null

    private var _binding: FragmentGetUserNumberBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGetUserNumberBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGenerateOTP.setOnClickListener {
            if (checkNumber()) {
                val phoneNumber = binding.countryCodePicker.selectedCountryCodeWithPlus + number
                sendCode(phoneNumber)

            }
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
//                firebaseAuth!!.signInWithCredential(credential).addOnCompleteListener {
//                    if (it.isSuccessful) {
//                        val userModel =
//                            UserModel(
//                                "", "", "",
//                                firebaseAuth!!.currentUser!!.phoneNumber!!,
//                                firebaseAuth!!.uid!!
//                            )
//
//                        databaseReference!!.child(firebaseAuth!!.uid!!).setValue(userModel)
//                        activity?.supportFragmentManager
//                            ?.beginTransaction()
//                            ?.replace(R.id.main_container, GetUserData())
//                            ?.commit()
//                    }
//                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException)
                    Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                else if (e is FirebaseTooManyRequestsException)
                    Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                else Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(
                verificationCode: String,
                p1: PhoneAuthProvider.ForceResendingToken
            ) {
                code = verificationCode
                activity!!.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_container, VerifyNumber.newInstance(code!!))
                    .commit()


            }
        }
    }


    private fun sendCode(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            phoneNumber,
            60,
            TimeUnit.SECONDS,
            requireActivity(),
            callbacks
        )
    }

    private fun checkNumber(): Boolean {
        number = binding.edtNumber.text.toString().trim()
        if (number!!.isEmpty()) {
            binding.edtNumber.error = "Field is required"
            return false
        } else if (number!!.length < 10) {
            binding.edtNumber.error = "Number should be 10 in length"
            return false
        } else return true
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}