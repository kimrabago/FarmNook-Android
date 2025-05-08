package com.ucb.capstone.farmnook.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ucb.capstone.farmnook.data.model.Message
import com.ucb.capstone.farmnook.utils.SendPushNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private lateinit var chatId: String
    private lateinit var receiverId: String
    lateinit var senderId: String

    fun initialize(chatId: String, receiverId: String) {
        this.chatId = chatId
        this.receiverId = receiverId
        this.senderId = auth.currentUser?.uid ?: ""
        loadMessages()
    }

    private fun loadMessages() {
        _isLoading.value = true
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                _isLoading.value = false

                if (error != null) {
                    _error.value = "Error loading messages"
                    return@addSnapshotListener
                }

                val tempList = mutableListOf<Message>()
                snapshots?.documents?.forEach { doc ->
                    val message = doc.toObject(Message::class.java)
                    message?.let {
                        val ts = when (val rawTs = doc.get("timestamp")) {
                            is Long -> rawTs
                            is Timestamp -> rawTs.toDate().time
                            else -> 0L
                        }
                        it.formattedTimestamp = formatTimestamp(ts)
                        tempList.add(it)
                    }
                }
                _messages.value = tempList
            }
    }

    fun sendMessage(content: String) {
        if (content.isEmpty()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val senderDoc = firestore.collection("users").document(senderId).get().await()
                val firstName = senderDoc.getString("firstName") ?: ""
                val lastName = senderDoc.getString("lastName") ?: ""
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
                ).await()

                // Add message to subcollection
                firestore.collection("chats").document(chatId)
                    .collection("messages")
                    .add(message)
                    .await()

                _messageSent.value = true
                SendPushNotification.sendMessageNotification(
                    receiverId = receiverId,
                    senderName = senderName,
                    message = content,
                    context = TODO()
                )
            } catch (e: Exception) {
                _error.value = "Failed to send message"
            } finally {
                _isLoading.value = false
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

    fun resetMessageSent() {
        _messageSent.value = false
    }

    fun clearError() {
        _error.value = null
    }
}