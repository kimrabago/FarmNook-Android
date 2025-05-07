package com.ucb.capstone.farmnook.ui.message


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Message
import com.ucb.capstone.farmnook.ui.adapter.MessageAdapter
import com.ucb.capstone.farmnook.utils.SendPushNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class MessageActivity : AppCompatActivity() {


    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var replyEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var receiverNameTextView: TextView

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()


    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    private lateinit var chatId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String
    private lateinit var senderId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)


        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        replyEditText = findViewById(R.id.replyEditText)
        sendButton = findViewById(R.id.sendButton)
        receiverNameTextView = findViewById(R.id.receiverName)




        // Get intent extras with null checks
        chatId = intent.getStringExtra("chatId") ?: run {
            Toast.makeText(this, "Chat not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }




        receiverId = intent.getStringExtra("recipientId") ?: run {
            Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener { finish() }




        receiverName = intent.getStringExtra("receiverName") ?: "Unknown User"
        receiverNameTextView.text = receiverName




        senderId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }




        // Setup RecyclerView
        messageAdapter = MessageAdapter(messageList, senderId)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageActivity)
            adapter = messageAdapter
        }




        loadMessages()




        sendButton.setOnClickListener {
            val messageText = replyEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }




    private fun loadMessages() {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }


                messageList.clear()
                snapshots?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    message?.let {
                        // Convert timestamp from Long or com.google.firebase.Timestamp
                        val ts = when (val rawTs = doc.get("timestamp")) {
                            is Long -> rawTs
                            is com.google.firebase.Timestamp -> rawTs.toDate().time
                            else -> 0L
                        }
                        it.formattedTimestamp = formatTimestamp(ts)
                        messageList.add(it)
                    }
                }


                messageAdapter.notifyDataSetChanged()
                messagesRecyclerView.scrollToPosition(messageList.size - 1)
            }
    }
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000


            val date = Date(timestamp)


            val sdfRecent = SimpleDateFormat("h:mm a", Locale.getDefault())
            val sdfOld = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) // e.g. "Apr 30, 4:45 PM"


            if (now - timestamp < oneDayMillis) {
                sdfRecent.format(date)
            } else {
                sdfOld.format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }




    private fun sendMessage(content: String) {
        firestore.collection("users").document(senderId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val senderName = "$firstName $lastName".trim()




                val message = Message(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    senderName = senderName
                )




                // Update chat document
                firestore.collection("chats").document(chatId).update(
                    "lastMessage", content,
                    "timestamp", System.currentTimeMillis()
                )




                // Add message to subcollection
                firestore.collection("chats").document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        replyEditText.text.clear()
                        SendPushNotification.sendMessageNotification(
                            context = this,
                            receiverId = receiverId,
                            senderName = senderName,
                            message = content
                        )
                    }
            }
    }
}


