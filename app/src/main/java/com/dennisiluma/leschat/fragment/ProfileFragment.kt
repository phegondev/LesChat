package com.dennisiluma.leschat.fragment

import android.app.Activity
import android.app.AlertDialog
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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dennisiluma.leschat.R
import com.dennisiluma.leschat.activity.EditNameActivity
import com.dennisiluma.leschat.constant.AppConstants
import com.dennisiluma.leschat.databinding.FragmentProfileBinding
import com.dennisiluma.leschat.permission.AppPermission
import com.dennisiluma.leschat.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView


class ProfileFragment : Fragment() {

    private lateinit var profileBinding: FragmentProfileBinding
    private lateinit var profileViewModels: ProfileViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var appPermission: AppPermission
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        profileBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        return profileBinding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appPermission = AppPermission()
        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)


        profileViewModels =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
                .create(ProfileViewModel::class.java)


        profileViewModels.getUser().observe(viewLifecycleOwner, Observer { userModel ->
            profileBinding.userModel = userModel //bind the values using data binding to the xml

            if (userModel.name.contains(" ")) {
                val split = userModel.name.split(" ")

                profileBinding.txtProfileFName.text = split[0]
                profileBinding.txtProfileLName.text = split[1]
            }

            profileBinding.cardName.setOnClickListener {
                val intent = Intent(context, EditNameActivity::class.java)
                intent.putExtra("name", userModel.name)
                startActivityForResult(intent, 100) //TODO("Clean deprecated")
            }
        });
        profileBinding.imgEditStatus.setOnClickListener {
            getStatusDialog()
        }

        profileBinding.imgPickImage.setOnClickListener {
            if (appPermission.isStorageOk(requireContext())) {//check for permission
                pickImage()
            } else appPermission.requestStoragePermission(requireActivity()) //ask for permission

        }
    }

    private fun getStatusDialog() {

        /*here we are creating a custom alert dialog*/
        val alertDialog = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_layout, null, false)
        alertDialog.setView(view) //help to replace the default dialog

        view.findViewById<Button>(R.id.btnEditStatus).setOnClickListener {
            val status = view.findViewById<EditText>(R.id.edtUserStatus).text.toString()
            if (status.isNotEmpty()) {
                profileViewModels.updateStatus(status)
                dialog.dismiss()
            }
        }
        dialog = alertDialog.create() //we are creating our custom dialog
        dialog.show()


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AppConstants.STORAGE_PERMISSION -> {

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    pickImage()
                else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickImage() {
        CropImage.activity().setCropShape(CropImageView.CropShape.OVAL)
            .start(requireContext(), this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            100 -> {
                if (data != null) {
                    val userName = data.getStringExtra("name")
                    profileViewModels.updateName(userName!!)
                    val editor = sharedPreferences.edit()
                    editor.putString("myName", userName).apply()
                }

            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                if (data != null) {

                    val result = CropImage.getActivityResult(data)
                    if (resultCode == Activity.RESULT_OK) {
                        uploadImage(result.uri)
                    }
                }
            }
        }
    }

    private fun uploadImage(imageUri: Uri) {
        //TODO("move code and make network calls in repository)
        storageReference = FirebaseStorage.getInstance().reference
        storageReference.child(firebaseAuth.uid + AppConstants.PATH).putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                val task = taskSnapshot.storage.downloadUrl
                task.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val imagePath = it.result.toString()

                        val editor = sharedPreferences.edit()
                        editor.putString("myImage", imagePath).apply() //this will be used in the message activity to display image of user in chat

                        profileViewModels.updateImage(imagePath)
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}