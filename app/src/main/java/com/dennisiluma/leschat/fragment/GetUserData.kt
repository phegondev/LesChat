package com.dennisiluma.leschat.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dennisiluma.leschat.activity.DashBoard
import com.dennisiluma.leschat.constant.AppConstants
import com.dennisiluma.leschat.databinding.FragmentGetUserDataBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

class GetUserData : Fragment() {
    private var image: Uri? = null
    private lateinit var username: String
    private lateinit var status: String
    private lateinit var imageUrl: String
    private var databaseReference: DatabaseReference? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var storageReference: StorageReference? = null

    private var _binding: FragmentGetUserDataBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentGetUserDataBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        storageReference = FirebaseStorage.getInstance().reference

        binding.btnDataDone.setOnClickListener {
            if (checkData()) {
                image?.let { it1 -> uploadData(username, status, it1) }
            }
        }
        binding.imgPickImage.setOnClickListener {
            if (checkStoragePermission())
                pickImage()
            else storageRequestPermission()
        }
    }

    private fun checkData(): Boolean {
        username = binding.edtUserName.text.toString().trim()
        status = binding.edtUserStatus.text.toString().trim()

        return if (username.isEmpty()) {
            binding.edtUserName.error = "Filed is required"
            false
        } else if (status.isEmpty()) {
            binding.edtUserStatus.error = "Filed is required"
            false
        } else if (image == null) {
            Toast.makeText(context, "Image required", Toast.LENGTH_SHORT).show()
            false
        } else true
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun storageRequestPermission() = ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), 1000
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    pickImage()
                else Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == Activity.RESULT_OK) {
                    image = result.uri
                    binding.imgUser.setImageURI(image)
                }
            }
        }
    }

    private fun uploadData(name: String, status: String, image: Uri) = kotlin.run {
        storageReference!!.child(firebaseAuth!!.uid + AppConstants.PATH).putFile(image)
            .addOnSuccessListener {
                val task =
                    it.storage.downloadUrl //download the url of the image present in the firebase image
                task.addOnCompleteListener { uri ->
                    imageUrl =
                        uri.result.toString() //save the firebase image path to this variable which is stored in the database reference at line 134
                    val map = mapOf(
                        "name" to name, //name node field as used in the real_time_database node to name_value that's being passed in
                        "status" to status,
                        "image" to imageUrl
                    )
                    databaseReference!!.child(firebaseAuth!!.uid!!).updateChildren(map)
                        .addOnCompleteListener {
                            shardPrefForSuccessfulProfileSetUp() // when the value is successfully logged in save to sharedprefer
                            startActivity(Intent(context, DashBoard::class.java))
                            requireActivity().finish()
                        }
                }
            }
    }

    fun shardPrefForSuccessfulProfileSetUp() {
        val sharedPref = requireActivity().getSharedPreferences(
            "registeredProfileSuccessfully",
            Context.MODE_PRIVATE
        )
        val editor = sharedPref.edit()
        editor.putString("registeredProfileSuccessfully", "registeredProfileSuccessfully")
        editor.apply()
    }


    private fun pickImage() {
        CropImage.activity()
            .setCropShape(CropImageView.CropShape.OVAL)
            .start(requireContext(), this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}