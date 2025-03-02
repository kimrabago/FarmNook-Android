package com.ucb.eldroid.farmnook.views.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.message.Message
import com.ucb.eldroid.farmnook.views.message.MessageActivity

class InboxAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<InboxAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
        private val messageContentTextView: TextView = itemView.findViewById(R.id.messageContentTextView)
        private val messageTimestampTextView: TextView = itemView.findViewById(R.id.messageTimestampTextView)
        private val personImageView: ImageView = itemView.findViewById(R.id.personImageView)

        fun bind(message: Message) {
            senderNameTextView.text = message.senderName
            messageContentTextView.text = message.messageContent
            messageTimestampTextView.text = message.timestamp

            message.avatarResId?.let {
                personImageView.setImageResource(it)
            }

            // When a message item is clicked, launch MessageActivity
            // and pass both the sender's name and the message content
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, MessageActivity::class.java)
                intent.putExtra("SENDER_NAME", message.senderName)
                intent.putExtra("MESSAGE_CONTENT", message.messageContent)
                context.startActivity(intent)
            }
        }
    }
}
