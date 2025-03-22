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
        inboxAdapter = InboxAdapter(chatList, requireContext())
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
            .orderBy("timestamp", Query.Direction.DESCENDING) // Sort by last activity
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    chatList.clear()
                    for (doc in value.documents) {
                        val userIds = doc.get("userIds") as? List<String> ?: continue
                        val otherUserId = userIds.firstOrNull { it != currentUserId } ?: continue
                        val lastMessage = doc.getString("lastMessage") ?: "No messages yet"
                        fetchUserDetails(doc.id, otherUserId, lastMessage)
                    }
                }
            }
    }

    private fun fetchUserDetails(chatId: String, otherUserId: String, lastMessage: String) {
        firestore.collection("users").document(otherUserId).get()
            .addOnSuccessListener { userDoc ->
                val name = "${userDoc.getString("firstName")} ${userDoc.getString("lastName")}"
                chatList.add(ChatItem(chatId, name, lastMessage))
                inboxAdapter.notifyDataSetChanged()
            }
    }
}
