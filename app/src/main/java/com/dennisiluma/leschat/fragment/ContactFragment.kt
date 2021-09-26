package com.dennisiluma.leschat.fragment

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.dennisiluma.leschat.model.UserModel
import com.dennisiluma.leschat.adapter.ContactAdapter
import com.dennisiluma.leschat.constant.AppConstants
import com.dennisiluma.leschat.databinding.FragmentContactBinding
import com.dennisiluma.leschat.permission.AppPermission
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ContactFragment : Fragment() {

    private lateinit var appPermission: AppPermission
    private lateinit var mobileContacts: ArrayList<UserModel>
    private lateinit var appContacts: ArrayList<UserModel>
    private var contactAdapter: ContactAdapter? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var phoneNumber: String

    private lateinit var fragmentContactBinding: FragmentContactBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentContactBinding = FragmentContactBinding.inflate(layoutInflater, container, false)
        return fragmentContactBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appPermission = AppPermission()
        firebaseAuth = FirebaseAuth.getInstance()
        phoneNumber = firebaseAuth.currentUser?.phoneNumber!!
//        phoneNumber = firebaseAuth.currentUser?.displayName!!

        if (appPermission.isContactOk(requireContext())) {
            getMobileContact()
        } else appPermission.requestContactPermission(requireActivity())

        fragmentContactBinding.contactSearchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (contactAdapter != null)
                    contactAdapter!!.filter.filter(newText)
                return false
            }
        })

    }

    @SuppressLint("Range")
    private fun getMobileContact() {

        val projection = arrayOf(
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        if (cursor != null) {
            mobileContacts = ArrayList()
            while (cursor.moveToNext()) {

                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var number =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                number = number.replace("\\s".toRegex(), "")
                val num = number.elementAt(0).toString()
                if (num == "0")
                    number = number.replaceFirst("(?:0)+".toRegex(), "+234")
                val userModel = UserModel()
                userModel.name = name
                userModel.number = number
                mobileContacts.add(userModel)
            }
            cursor.close()
            getAppContact(mobileContacts)
        }
    }

    private fun getAppContact(mobileContact: ArrayList<UserModel>) {

        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        val query = databaseReference.orderByChild("number")
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    appContacts = ArrayList()
                    /*This block of code is big(0)N,  optimize it to logN*/
                    for (data in snapshot.children) {

                        val number = data.child("number").value.toString()
                        for (mobileModel in mobileContact) {
                            if ((mobileModel.number == number) && (number != phoneNumber)) {
                                val userModel = data.getValue(UserModel::class.java)
                                appContacts.add(userModel!!)
                            }
                        }
                    }

                    fragmentContactBinding.recyclerViewContact.apply {
                        layoutManager = LinearLayoutManager(context)
                        setHasFixedSize(true)
                        contactAdapter = ContactAdapter(appContacts)
                        adapter = contactAdapter
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AppConstants.CONTACT_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    getMobileContact()
                else Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}