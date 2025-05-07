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


        firestore.collection("chats")
            .whereArrayContains("userIds", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }


                chatList.clear()
                value?.documents?.forEach { doc ->
                    val userIds = doc.get("userIds") as? List<String> ?: return@forEach
                    val otherUserId = userIds.firstOrNull { it != currentUserId } ?: return@forEach


                    fetchUserDetails(
                        chatId = doc.id,
                        otherUserId = otherUserId,
                        lastMessage = doc.getString("lastMessage") ?: "No messages yet",
                        timestamp = doc.getLong("timestamp") ?: 0L
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
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val fullName = "$firstName $lastName".trim()
                val profileImage = userDoc.getString("profileImageUrl") ?: ""


                chatList.add(ChatItem(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    profileImageUrl = profileImage,
                    userName = fullName,
                    lastMessage = lastMessage,
                    timestamp = timestamp
                ))
                inboxAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Fallback if user details can't be fetched
                chatList.add(ChatItem(
                    chatId = chatId,
                    otherUserId = otherUserId,
                    profileImageUrl = "",
                    userName = "Unknown User",
                    lastMessage = lastMessage,
                    timestamp = timestamp
                ))
                inboxAdapter.notifyDataSetChanged()
            }
    }
}
