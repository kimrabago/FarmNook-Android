package com.ucb.eldroid.farmnook.views.message

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R

class InboxFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_inbox, container, false)

        // Initialize Views
        val btnBack: ImageButton = view.findViewById(R.id.btn_back)
        val btnNewMessage: ImageButton = view.findViewById(R.id.new_message_btn)
        val recyclerView: RecyclerView = view.findViewById(R.id.messagesRecyclerView)

        // Handle Back Button Click
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Handle New Message Button Click (Open NewMessageActivity)
        btnNewMessage.setOnClickListener {
            val intent = Intent(requireContext(), NewMessageActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}
