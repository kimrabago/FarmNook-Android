package com.ucb.capstone.farmnook.ui.message


import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Message
import com.ucb.capstone.farmnook.ui.adapter.MessageAdapter
import com.ucb.capstone.farmnook.utils.SendPushNotification
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MessageActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var replyEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var receiverNameTextView: TextView
    private lateinit var messageIcon: ImageButton


    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var chatId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String
    private lateinit var senderId: String
    private lateinit var senderName: String


    private val GALLERY_REQUEST_CODE = 2001


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        initializeViews()
        initializeChatDetails()
        setupRecyclerView()
        loadMessages()
        setupClickListeners()
    }


    private fun initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        replyEditText = findViewById(R.id.replyEditText)
        sendButton = findViewById(R.id.sendButton)
        receiverNameTextView = findViewById(R.id.receiverName)
        messageIcon = findViewById(R.id.messageIcon)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    }


    private fun initializeChatDetails() {
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

        receiverName = intent.getStringExtra("receiverName") ?: "Unknown User"
        receiverNameTextView.text = receiverName


        senderId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        // Get sender name from Firestore or pass it from previous activity
        firestore.collection("users").document(senderId).get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                senderName = "$firstName $lastName".trim()
            }
    }


    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList, senderId)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }


    private fun loadMessages() {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("MessageActivity", "Error loading messages", error)
                    return@addSnapshotListener
                }


                messageList.clear()
                snapshots?.documents?.forEach { doc ->
                    // MANUALLY PARSE FIELDS INSTEAD OF USING toObject()
                    val senderId = doc.getString("senderId") ?: ""
                    val receiverId = doc.getString("receiverId") ?: ""
                    val content = doc.getString("content") ?: ""
                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is com.google.firebase.Timestamp -> ts.toDate().time // Convert Firestore Timestamp to Long
                        is Long -> ts // Already a Long
                        else -> 0L // Fallback
                    }
                    val senderName = doc.getString("senderName") ?: ""
                    val imageUrl = doc.getString("imageUrl")


                    val message = Message(
                        senderId = senderId,
                        receiverId = receiverId,
                        content = content,
                        timestamp = timestamp,
                        senderName = senderName,
                        imageUrl = imageUrl
                    ).apply {
                        formattedTimestamp = formatTimestamp(timestamp)
                    }


                    messageList.add(message)
                }


                messageAdapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messageList.size - 1)
                }
            }
    }


    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000


            val date = Date(timestamp)
            val sdfRecent = SimpleDateFormat("h:mm a", Locale.getDefault())
            val sdfOld = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())


            if (now - timestamp < oneDayMillis) {
                sdfRecent.format(date)
            } else {
                sdfOld.format(date)
            }
        } catch (e: Exception) {
            ""
        }
    }


    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val messageText = replyEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }


        messageIcon.setOnClickListener {
            openGallery()
        }
    }


    private fun sendMessage(content: String, imageUrl: String? = null) {
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            senderName = senderName,
            imageUrl = imageUrl
        )


        // Update chat document
        firestore.collection("chats").document(chatId).update(
            "lastMessage", content,
            "timestamp", FieldValue.serverTimestamp()
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
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }



    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                uploadImageToFirebaseStorage(uri)
            }
        }
    }


    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // 50% quality
        return outputStream.toByteArray()
    }


    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        val storageRef = storage.reference
        val fileName = "messages/${System.currentTimeMillis()}.jpg"
        val imageRef = storageRef.child(fileName)


        // Show progress
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending image...")
        progressDialog.setCancelable(false)
        progressDialog.show()


        // Compress image
        val data = compressImage(imageUri)


        imageRef.putBytes(data)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    sendMessage("Sent an image", uri.toString())
                    progressDialog.dismiss()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
