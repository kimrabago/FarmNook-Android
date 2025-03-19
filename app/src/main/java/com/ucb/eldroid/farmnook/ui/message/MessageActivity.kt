package com.ucb.eldroid.farmnook.ui.message

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.data.model.Message
import com.ucb.eldroid.farmnook.ui.adapter.MessageAdapter

class MessageActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var replyEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var receiverNameTextView: TextView

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var senderId: String? = null
    private var receiverId: String? = null
    private var receiverName: String? = null
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Closes the activity and goes back
        }

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        replyEditText = findViewById(R.id.replyEditText)
        sendButton = findViewById(R.id.sendButton)
        receiverNameTextView = findViewById(R.id.receiverName)

        senderId = auth.currentUser?.uid
        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")

        if (senderId == null || receiverId == null) {
            Toast.makeText(this, "Error: Missing user details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        receiverNameTextView.text = receiverName

        // Generate chat ID
        chatId = if (senderId!! < receiverId!!) {
            "$senderId-$receiverId"
        } else {
            "$receiverId-$senderId"
        }

        // Setup RecyclerView
        messageAdapter = MessageAdapter(messageList, senderId!!)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messageAdapter

        // Load messages
        loadMessages()

        // Send message
        sendButton.setOnClickListener {
            fetchSenderNameAndSendMessage()
        }
    }

    private fun loadMessages() {
        firestore.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("MessageActivity", "Error loading messages", error)
                    return@addSnapshotListener
                }

                messageList.clear()
                snapshots?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    if (message != null) messageList.add(message)
                }

                messageAdapter.notifyDataSetChanged()
                messagesRecyclerView.scrollToPosition(messageList.size - 1)
            }
    }

    private fun fetchSenderNameAndSendMessage() {
        val text = replyEditText.text.toString().trim()
        if (text.isEmpty()) return

        firestore.collection("users").document(senderId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val senderFullName = "$firstName $lastName".trim()

                    sendMessage(text, senderFullName)
                } else {
                    Toast.makeText(this, "Sender profile not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch sender name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessage(content: String, senderName: String) {
        val message = Message(senderId!!, receiverId!!, content, System.currentTimeMillis(), senderName)

        val chatRef = firestore.collection("chats").document(chatId)

        // Ensure the chat exists before adding messages
        chatRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val chatData = hashMapOf(
                    "userIds" to listOf(senderId, receiverId)
                )
                chatRef.set(chatData)
            }

            // Add message to Firestore
            chatRef.collection("messages").add(message)
                .addOnSuccessListener {
                    replyEditText.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
