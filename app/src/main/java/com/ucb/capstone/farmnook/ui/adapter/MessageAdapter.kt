package com.ucb.capstone.farmnook.ui.adapter




import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Message




class MessageAdapter(private var messageList: List<Message>, private val currentUserId: String) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = if (viewType == 1)
            LayoutInflater.from(parent.context).inflate(R.layout.message_item_sent, parent, false)
        else
            LayoutInflater.from(parent.context).inflate(R.layout.message_item_received, parent, false)
        return MessageViewHolder(view)
    }


    fun updateMessages(newMessages: List<Message>) {
        this.messageList = newMessages
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]


        holder.messageContentTextView.text = message.content
        holder.messageTimestampTextView.text = message.formattedTimestamp


        // Load image with Glide if imageUrl is not null/empty
        if (!message.imageUrl.isNullOrEmpty()) {
            holder.messageImageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(message.imageUrl)
                .into(holder.messageImageView)
        } else {
            holder.messageImageView.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int = messageList.size


    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) 1 else 0
    }


    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageContentTextView: TextView = itemView.findViewById(R.id.messageContentTextView)
        val messageTimestampTextView: TextView = itemView.findViewById(R.id.messageTimestampTextView)
        val messageImageView: ImageView = itemView.findViewById(R.id.messageImageView) // 👈 Add this
    }


}

