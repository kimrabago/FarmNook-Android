package com.ucb.capstone.farmnook.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.data.model.Notification
import com.ucb.capstone.farmnook.databinding.ItemNotificationBinding

class NotificationAdapter(
    private var items: List<Notification>,
    private val onClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Notification) {
            binding.notificationTitle.text = item.title
            binding.notificationMessage.text = item.message
            binding.notificationTimestamp.text = item.timestamp

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationBinding.inflate(inflater, parent, false)
        return NotificationViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])


    }

    fun updateList(newItems: List<Notification>) {
        items = newItems
        notifyDataSetChanged()
    }
}

