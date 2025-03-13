package com.ucb.eldroid.farmnook.views.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.User
import com.ucb.eldroid.farmnook.views.message.MessageActivity

class UserAdapter(private val usersList: List<User>, private val context: Context) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
    return UserViewHolder(view)
}

override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
    val user = usersList[position]

    val fullName = "${user.firstName} ${user.lastName}"
    holder.senderNameTextView.text = fullName
    holder.messageContentTextView.text = user.userType // Display user type (Farmer/Hauler)
    holder.messageTimestampTextView.visibility = View.GONE // Hide timestamp for user list

    // Load user profile image if available
    if (!user.profileImage.isNullOrEmpty()) {
        Glide.with(context).load(user.profileImage).into(holder.personImageView)
    } else {
        holder.personImageView.setImageResource(R.drawable.profile_circle)
    }

    holder.itemView.setOnClickListener {
        val intent = Intent(context, MessageActivity::class.java)
        intent.putExtra("receiverId", user.userId)
        context.startActivity(intent)
    }
}

override fun getItemCount(): Int = usersList.size

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val senderNameTextView: TextView = itemView.findViewById(R.id.senderNameTextView)
    val messageContentTextView: TextView = itemView.findViewById(R.id.messageContentTextView)
    val messageTimestampTextView: TextView = itemView.findViewById(R.id.messageTimestampTextView)
    val personImageView: ImageView = itemView.findViewById(R.id.personImageView)
}
}