package com.ucb.eldroid.farmnook.views.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.NotificationItem

class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val haulerName: TextView = itemView.findViewById(R.id.haulerName)
        private val notifMessage: TextView = itemView.findViewById(R.id.notification_message)
        private val dateTime: TextView = itemView.findViewById(R.id.date_time)

        fun bind(item: NotificationItem) {
            haulerName.text = item.userName
            notifMessage.text = item.notifMessage
            dateTime.text = item.dateTime
            // Optionally, load an image into profileImage if needed.
        }
    }
}
