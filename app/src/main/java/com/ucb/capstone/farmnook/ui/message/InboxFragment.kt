package com.ucb.capstone.farmnook.ui.message

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.adapter.InboxAdapter
import com.ucb.capstone.farmnook.data.model.ChatItem

class InboxFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inboxAdapter: InboxAdapter
    private lateinit var chatList: MutableList<ChatItem>

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inbox, container, false)

        recyclerView = view.findViewById(R.id.messagesRecyclerView)
        val newMessageBtn = view.findViewById<ImageButton>(R.id.new_message_btn)

        chatList = mutableListOf()
        inboxAdapter = InboxAdapter(chatList) { chatItem ->
            Intent(requireContext(), MessageActivity::class.java).apply {
                putExtra("chatId", chatItem.chatId)
                putExtra("recipientId", chatItem.otherUserId)
                putExtra("receiverName", chatItem.userName)
            }.also { startActivity(it) }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = inboxAdapter

        newMessageBtn.setOnClickListener {
            startActivity(Intent(requireContext(), NewMessageActivity::class.java))
        }

        fetchChats()

        return view
    }

    private fun fetchChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        Log.d("InboxFragment", "Fetching chats for user: $currentUserId")

        firestore.collection("chats")
            .whereArrayContains("userIds", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Log.e("InboxFragment", "Error fetching chats", error)
                    return@addSnapshotListener
                }

                chatList.clear() // Clearing chat list before adding new data
                value?.documents?.forEach { doc ->
                    val userIds = doc.get("userIds") as? List<String> ?: return@forEach
                    val otherUserId = userIds.firstOrNull { it != currentUserId } ?: return@forEach

                    // Safely get the timestamp and handle different formats
                    val timestamp: Long = when (val timestampValue = doc.get("timestamp")) {
                        is com.google.firebase.Timestamp -> timestampValue.seconds * 1000 // Convert to milliseconds
                        is Long -> timestampValue // If it's already a Long, use it
                        is String -> timestampValue.toLongOrNull() ?: 0L // If it's a String, try converting it to Long
                        else -> 0L // Default to 0 if it's null or an unexpected type
                    }

                    // Fetch user details
                    fetchUserDetails(
                        chatId = doc.id,
                        otherUserId = otherUserId,
                        lastMessage = doc.getString("lastMessage") ?: "No messages yet",
                        timestamp = timestamp
                    )
                }
            }
    }

    private fun fetchUserDetails(
        chatId: String,
        otherUserId: String,
        lastMessage: String,
        timestamp: Long
    ) {
        firestore.collection("users").document(otherUserId).get()
            .addOnSuccessListener { userDoc ->
                val firstName = userDoc.getString("firstName") ?: "Unknown"
                val lastName = userDoc.getString("lastName") ?: "User"
                val fullName = "$firstName $lastName".trim()
                val profileImage =
                    userDoc.getString("profileImageUrl") ?: "" // Default to empty string if null

                Log.d("InboxFragment", "Fetched user details: $fullName")

                // Add chat item and notify the adapter
                val chatItem = ChatItem(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    profileImageUrl = profileImage,
                    userName = fullName,
                    lastMessage = lastMessage,
                    timestamp = timestamp
                )

                // Run on main thread for UI updates
                activity?.runOnUiThread {
                    chatList.add(chatItem)
                    inboxAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("InboxFragment", "Failed to fetch user details", exception)

                // Fallback values for unknown users
                val fallbackChatItem = ChatItem(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    profileImageUrl = "", // No image available
                    userName = "Unknown User",
                    lastMessage = lastMessage,
                    timestamp = timestamp
                )

                // Run on main thread for UI updates
                activity?.runOnUiThread {
                    chatList.add(fallbackChatItem)
                    inboxAdapter.notifyDataSetChanged()
                }
            }
    }
}
