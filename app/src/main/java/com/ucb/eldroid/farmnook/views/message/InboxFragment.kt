package com.ucb.eldroid.farmnook.views.message

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.adapter.InboxAdapter

class InboxFragment : Fragment() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var inboxAdapter: InboxAdapter
    private val messageList = mutableListOf<Message>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_inbox, container, false)

        val btnNewMessage: ImageButton = view.findViewById(R.id.new_message_btn)
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView)

        // Set up RecyclerView with a vertical LinearLayoutManager
        messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter with an empty list; dummy data will be loaded
        inboxAdapter = InboxAdapter(messageList)
        messagesRecyclerView.adapter = inboxAdapter

        // Load dummy data into the message list
        loadDummyData()

        // Handle "New Message" button click
        btnNewMessage.setOnClickListener {
            val intent = Intent(requireContext(), NewMessageActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun loadDummyData() {
        val dummyMessages = listOf(
            Message(
                senderName = "Michael Jackstone",
                messageContent = "Heeee-heeeee",
                timestamp = "12:00 AM",
                avatarResId = R.drawable.profile_circle
            ),
            Message(
                senderName = "Megan Miming",
                messageContent = "My truck is from transformers.",
                timestamp = "3:20 PM",
                avatarResId = R.drawable.profile_circle
            ),
            Message(
                senderName = "Phato Thuya",
                messageContent = "Heeee-heeeee",
                timestamp = "7:10 PM",
                avatarResId = R.drawable.profile_circle
            ),
            Message(
                senderName = "Joji Ann Santos",
                messageContent = "Sir san poba ang bahay ng pagde-deliveran...",
                timestamp = "12:18 PM",
                avatarResId = R.drawable.profile_circle
            )
        )

        messageList.clear()
        messageList.addAll(dummyMessages)
        inboxAdapter.notifyDataSetChanged()
    }
}
