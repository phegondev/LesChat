package com.dennisiluma.leschat.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dennisiluma.leschat.BR
import com.dennisiluma.leschat.constant.AppConstants
import com.dennisiluma.leschat.databinding.ActivityMessageBinding
import com.dennisiluma.leschat.databinding.LeftItemLayoutBinding
import com.dennisiluma.leschat.databinding.RightItemLayoutBinding
import com.dennisiluma.leschat.model.ChatListModel
import com.dennisiluma.leschat.model.MessageModel
import com.dennisiluma.leschat.model.UserModel
import com.dennisiluma.leschat.permission.AppPermission
import com.dennisiluma.leschat.services.SendMediaService
import com.dennisiluma.leschat.util.AppUtil
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.fxn.pix.Options
import com.fxn.pix.Pix
import com.fxn.utility.PermUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

class MessageActivity : AppCompatActivity() {
    private lateinit var activityMessageBinding: ActivityMessageBinding

    private var hisId: String? = null
    private var hisImage: String? = null
    private var chatId: String? = null
    private var myName: String? = null
    private lateinit var appUtil: AppUtil
    private lateinit var myId: String
    private lateinit var myImage: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appPermission: AppPermission
    private var firebaseRecyclerAdapter: FirebaseRecyclerAdapter<MessageModel, ViewHolder>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMessageBinding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(activityMessageBinding.root)

        appUtil = AppUtil()
        myId = appUtil.getUID()!!
        sharedPreferences = getSharedPreferences("userData", MODE_PRIVATE)
        myImage = sharedPreferences.getString("myImage", "").toString()
        myName = sharedPreferences.getString("myName", "").toString()
        appPermission = AppPermission()

        activityMessageBinding.activity = this

        if (intent.hasExtra("chatId")) {

            chatId = intent.getStringExtra("chatId")
            hisId = intent.getStringExtra("hisId")
            hisImage = intent.getStringExtra("hisImage")
            readMessages(chatId!!)

        } else {
            hisId = intent.getStringExtra("hisId")
            hisImage = intent.getStringExtra("hisImage")
        }

        activityMessageBinding.hisImage = hisImage

        if (chatId == null) { //when we navigate here form chat adapter we don't know weather the user has a chat on ground already. If he does, we set the chaid to the chatId
            checkOnlineStatus()
            hisId?.let { checkChat(it) } //here we are using the hisID to check if he has chat with the current user, if yes update the chatId which is currently null
        }
        activityMessageBinding.btnSend.setOnClickListener {
            val message = activityMessageBinding.msgText.text.toString()
            if (message.isEmpty())
                Toast.makeText(this, "Enter Message", Toast.LENGTH_SHORT).show()
            else {
                sendMessage(message)
                getToken(message)
            }
        }

        activityMessageBinding.btnSend.setOnClickListener {
            val message = activityMessageBinding.msgText.text.toString()
            if (message.isEmpty())
                Toast.makeText(this, "Enter Message", Toast.LENGTH_SHORT).show()
            else {
                sendMessage(message)
            }
        }

        activityMessageBinding.msgText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isEmpty())
                    typingStatus("false")
                else
                    typingStatus(hisId!!)
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        activityMessageBinding.btnDataSend.setOnClickListener {
            pickImage()
        }

    }

    /*This method help to check if the user has a chat with this individual, if he has set the chatId value to the chatId*/
    private fun checkChat(hisId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("ChatList").child(myId)
        val query = databaseReference.orderByChild("member").equalTo(hisId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (ds in snapshot.children) {
                        val member = ds.child("member").value.toString()
                        if (member == hisId) {
                            chatId =
                                ds.key // ds.key is a unique key coined from myId && hisId
                            readMessages(chatId!!)

                            break
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun createChat(message: String) {

        var databaseReference = FirebaseDatabase.getInstance().getReference("ChatList")
            .child(myId) //create a node for yourself
        chatId = databaseReference.push().key //generate a unique keyID for this chat

        val chatListMode =
            ChatListModel(chatId!!, message, System.currentTimeMillis().toString(), hisId!!)

        databaseReference.child(chatId!!)
            .setValue(chatListMode) //create a child for yourself using the generated chatId and set the column parameter vlues

        databaseReference = FirebaseDatabase.getInstance().getReference("ChatList")
            .child(hisId!!) //create a node for that particular user with that id, so he woun't recreate it agin by himself

        val chatList =
            ChatListModel(chatId!!, message, System.currentTimeMillis().toString(), myId)

        databaseReference.child(chatId!!)
            .setValue(chatList) // create a child for the other person using the generated chatID

        databaseReference = FirebaseDatabase.getInstance().getReference("Chat")
            .child(chatId!!) //setUp a chat for you both

        val messageModel = MessageModel(myId, hisId!!, message, type = "text")
        databaseReference.push().setValue(messageModel)
    }

    private fun sendMessage(message: String) {

        if (chatId == null)
            createChat(message)
        else {
            var databaseReference =
                FirebaseDatabase.getInstance().getReference("Chat").child(chatId!!)

            val messageModel =
                MessageModel(myId, hisId!!, message, System.currentTimeMillis().toString(), "text")

            databaseReference.push().setValue(messageModel)

            /*Update last message in chat list node*/
            val map: MutableMap<String, Any> = HashMap()

            map["lastMessage"] = message
            map["date"] = System.currentTimeMillis().toString()

            databaseReference =
                FirebaseDatabase.getInstance().getReference("ChatList").child(myId).child(chatId!!)

            databaseReference.updateChildren(map)

            databaseReference =
                FirebaseDatabase.getInstance().getReference("ChatList").child(hisId!!)

                    .child(chatId!!)
            databaseReference.updateChildren(map)
        }
    }

    private fun readMessages(chatId: String) {

        val query = FirebaseDatabase.getInstance().getReference("Chat").child(chatId)

        val firebaseRecyclerOptions = FirebaseRecyclerOptions.Builder<MessageModel>()
            .setLifecycleOwner(this)
            .setQuery(query, MessageModel::class.java)
            .build()
        query.keepSynced(true)

        firebaseRecyclerAdapter =
            object : FirebaseRecyclerAdapter<MessageModel, ViewHolder>(firebaseRecyclerOptions) {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

                    var viewDataBinding: ViewDataBinding? = null

                    if (viewType == 0)
                        viewDataBinding = RightItemLayoutBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )

                    if (viewType == 1)

                        viewDataBinding = LeftItemLayoutBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )

                    return ViewHolder(viewDataBinding!!)

                }

                override fun onBindViewHolder(
                    holder: ViewHolder,
                    position: Int,
                    messageModel: MessageModel
                ) {
                    if (getItemViewType(position) == 0) {
                        holder.viewDataBinding.setVariable(BR.message, messageModel)
                        holder.viewDataBinding.setVariable(BR.messageImage, myImage)
                    }

                    if (getItemViewType(position) == 1) {
                        holder.viewDataBinding.setVariable(BR.message, messageModel)
                        holder.viewDataBinding.setVariable(BR.messageImage, hisImage)
                    }
                }

                override fun getItemViewType(position: Int): Int {
                    val messageModel = getItem(position)
                    return if (messageModel.senderId == myId)
                        0
                    else
                        1
                }
            }

        activityMessageBinding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
        activityMessageBinding.messageRecyclerView.adapter = firebaseRecyclerAdapter
        firebaseRecyclerAdapter!!.startListening()

    }

    class ViewHolder(var viewDataBinding: ViewDataBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root)

    private fun typingStatus(typing: String) {

        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(myId)
        val map = HashMap<String, Any>()
        map["typing"] = typing
        databaseReference.updateChildren(map)

    }

    private fun checkOnlineStatus() {

        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(hisId!!)
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    activityMessageBinding.online = userModel?.online

                    val typing = userModel?.typing

                    if (typing == myId) {
                        activityMessageBinding.lottieAnimation.visibility = View.VISIBLE
                        activityMessageBinding.lottieAnimation.playAnimation()
                    } else {
                        activityMessageBinding.lottieAnimation.cancelAnimation()
                        activityMessageBinding.lottieAnimation.visibility = View.GONE

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

        /**first generate a message token then send message by calling the sendNotification method*/
    private fun getToken(message: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(hisId!!)
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val token = snapshot.child("token").value.toString()

                    val to = JSONObject()
                    val data = JSONObject()

                    data.put("hisId", myId)
                    data.put("hisImage", myImage)
                    data.put("title", myName)
                    data.put("message", message)
                    data.put("chatId", chatId)

                    to.put("to", token)
                    to.put("data", data)
                    sendNotification(to)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }


    private fun sendNotification(to: JSONObject) {

        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            AppConstants.NOTIFICATION_URL,
            to,
            Response.Listener { response: JSONObject ->

                Log.d("TAG", "onResponse: $response")
            },
            Response.ErrorListener {

                Log.d("TAG", "onError: $it")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val map: MutableMap<String, String> = HashMap()

                map["Authorization"] = "key=" + AppConstants.SERVER_KEY
                map["Content-type"] = "application/json"
                return map
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        request.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)

    }

    private fun pickImage() {

        val options: Options = Options.init()
            .setRequestCode(100)
            .setCount(5)
            .setFrontfacing(true)
            .setSpanCount(4)
            .setExcludeVideos(true)
            .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)
            .setPath("/les chat/Media/Sent")


        Pix.start(this@MessageActivity, options)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == 100) {

            val returnValue = data?.getStringArrayListExtra(Pix.IMAGE_RESULTS)

            if (chatId == null)
                Toast.makeText(this, "Please send text message first", Toast.LENGTH_SHORT).show()
            else {
                Toast.makeText(this, "Called", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, SendMediaService::class.java)
                intent.putExtra("hisID", hisId)
                intent.putExtra("chatID", chatId)
                intent.putStringArrayListExtra("media", returnValue)

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
                    startForegroundService(intent)
                else
                    startService(intent)
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermUtil.REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS -> {


                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImage()
                } else {
                    Toast.makeText(
                        this@MessageActivity,
                        "Approve permissions to open Pix ImagePicker",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }


    override fun onPause() {
        super.onPause()
        if (firebaseRecyclerAdapter != null)
            firebaseRecyclerAdapter!!.stopListening()
        appUtil.updateOnlineStatus("offline")
    }

    override fun onResume() {
        super.onResume()
        appUtil.updateOnlineStatus("online")
    }

    fun userInfo() {
        val intent = Intent(this, UserInfoActivity::class.java)
        intent.putExtra("userId", hisId)
        startActivity(intent)
    }
}