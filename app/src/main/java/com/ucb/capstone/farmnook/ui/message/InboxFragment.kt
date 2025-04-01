package com.ucb.capstone.farmnook.ui.message

import android.content.Intent
import android.os.Bundle
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inbox, container, false)

        recyclerView = view.findViewById(R.id.messagesRecyclerView)
        val newMessageBtn = view.findViewById<ImageButton>(R.id.new_message_btn)

        chatList = mutableListOf()
        // Pass a click callback to the adapter so we can open the chat
        inboxAdapter = InboxAdapter(chatList) { chatItem ->
            // When user clicks an item, open MessageActivity
            val intent = Intent(requireContext(), MessageActivity::class.java)
            intent.putExtra("receiverId", chatItem.otherUserId)
            intent.putExtra("receiverName", chatItem.userName)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = inboxAdapter

        newMessageBtn.setOnClickListener {
            val intent = Intent(requireContext(), NewMessageActivity::class.java)
            startActivity(intent)
        }

        fetchChats()

        return view
    }

    private fun fetchChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .whereArrayContains("userIds", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    chatList.clear()
                    for (doc in value.documents) {
                        val userIds = doc.get("userIds") as? List<String> ?: continue
                        val otherUserId = userIds.firstOrNull { it != currentUserId } ?: continue

                        val lastMessage = doc.getString("lastMessage") ?: "No messages yet"
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        // We'll fetch the user's name, then add to chatList
                        fetchUserDetails(doc.id, otherUserId, lastMessage, timestamp)
                    }
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
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val fullName = "$firstName $lastName".trim()

                val chatItem = ChatItem(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    userName = fullName,
                    lastMessage = lastMessage,
                    timestamp = timestamp
                )
                chatList.add(chatItem)
                inboxAdapter.notifyDataSetChanged()
            }
    }
}