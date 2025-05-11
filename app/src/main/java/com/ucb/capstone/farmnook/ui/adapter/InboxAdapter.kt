package com.ucb.capstone.farmnook.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.ChatItem
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import com.ucb.capstone.farmnook.utils.loadImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InboxAdapter(
    private val chatList: List<ChatItem>,
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val chatItem = chatList[position]
        holder.bind(chatItem)

        holder.itemView.setOnClickListener {
            onItemClick(chatItem)
        }
    }

    override fun getItemCount(): Int = chatList.size

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val personImageView: ShapeableImageView = itemView.findViewById(R.id.personImageView)
        private val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
        private val messageContentTextView: TextView = itemView.findViewById(R.id.messageContentTextView)
        private val messageTimestampTextView: TextView = itemView.findViewById(R.id.messageTimestampTextView)

        fun bind(chatItem: ChatItem) {
            personImageView.loadImage(chatItem.profileImageUrl)
            senderNameTextView.text = chatItem.userName
            messageContentTextView.text = chatItem.lastMessage

            if (chatItem.timestamp > 0) {
                messageTimestampTextView.text = formatTimestamp(chatItem.timestamp)
            } else {
                messageTimestampTextView.text = ""
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val currentTime = System.currentTimeMillis()
            val diffInMillis = currentTime - timestamp

            return if (diffInMillis > 24 * 60 * 60 * 1000) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            } else {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
