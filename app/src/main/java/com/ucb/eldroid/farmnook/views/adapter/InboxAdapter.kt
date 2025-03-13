package com.ucb.eldroid.farmnook.views.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.ChatItem
import com.ucb.eldroid.farmnook.views.message.MessageActivity

class InboxAdapter(private val chatList: MutableList<ChatItem>, private val context: Context) :
    RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val chatId = chatList[position]

        FirebaseFirestore.getInstance().collection("chats").document(chatId.toString()).collection("messages")
            .orderBy("timestamp")
            .limitToLast(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val message = snapshot.documents[0]
                    holder.senderNameTextView.text = message.getString("senderName")
                }
            }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MessageActivity::class.java)
            intent.putExtra("chatId", chatId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = chatList.size

    class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
    }
}

private fun Intent.putExtra(s: String, chatId: ChatItem) {

}
