package com.dennisiluma.leschat.activity

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dennisiluma.leschat.BR
import com.dennisiluma.leschat.databinding.ActivityMessageBinding
import com.dennisiluma.leschat.databinding.LeftItemLayoutBinding
import com.dennisiluma.leschat.databinding.RightItemLayoutBinding
import com.dennisiluma.leschat.model.ChatListModel
import com.dennisiluma.leschat.model.MessageModel
import com.dennisiluma.leschat.permission.AppPermission
import com.dennisiluma.leschat.util.AppUtil
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        if (chatId == null) { //when we navigate here form chat adapter we don't know weather the user has a chat on ground already. If he does, we set the chaid to the chatId
            hisId?.let { checkChat(it) } //here we are using the hisID to check if he has chat with the current user, if yes update the chatId which is currently null
        }
        activityMessageBinding.btnSend.setOnClickListener {
            val message = activityMessageBinding.msgText.text.toString()
            if (message.isEmpty())
                Toast.makeText(this, "Enter Message", Toast.LENGTH_SHORT).show()
            else {
                sendMessage(message)
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

    override fun onPause() {
        super.onPause()
        if (firebaseRecyclerAdapter != null)
            firebaseRecyclerAdapter!!.stopListening()
        //appUtil.updateOnlineStatus("offline")
    }

    fun userInfo() {
        val intent = Intent(this, UserInfoActivity::class.java)
        intent.putExtra("userId", hisId)
        startActivity(intent)
    }
}