package com.ucb.capstone.farmnook.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.ChatItem
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class InboxAdapter(
    private val chatList: List<ChatItem>,
    // This lambda is triggered when a chat item is clicked
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        // Inflate your custom XML (the card-based layout)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val chatItem = chatList[position]
        holder.bind(chatItem)

        // Handle clicks
        holder.itemView.setOnClickListener {
            onItemClick(chatItem)
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Match these with the IDs in your XML layout
        private val personImageView: ShapeableImageView = itemView.findViewById(R.id.personImageView)
        private val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
        private val messageContentTextView: TextView = itemView.findViewById(R.id.messageContentTextView)
        private val messageTimestampTextView: TextView = itemView.findViewById(R.id.messageTimestampTextView)

        fun bind(chatItem: ChatItem) {
            // Display the user's name
            senderNameTextView.text = chatItem.userName

            // Display the last message
            messageContentTextView.text = chatItem.lastMessage

            // Format and display the timestamp, if present
            if (chatItem.timestamp > 0) {
                messageTimestampTextView.text = formatTimestamp(chatItem.timestamp)
            } else {
                messageTimestampTextView.text = ""
            }

            // If you want to load an image from a URL or Firebase Storage:
            // Glide.with(personImageView)
            //     .load(chatItem.profileImageUrl)
            //     .placeholder(R.drawable.profile_circle)
            //     .into(personImageView)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
}
