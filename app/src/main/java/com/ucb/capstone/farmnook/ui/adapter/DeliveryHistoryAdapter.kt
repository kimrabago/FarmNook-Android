package com.ucb.capstone.farmnook.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import java.text.SimpleDateFormat
import java.util.*

class DeliveryHistoryAdapter(
    private val items: List<DeliveryHistory>,
    private val farmerNames: Map<String, String>, // Map<deliveryId, farmerName>
    private val farmerImages: Map<String, String?>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DeliveryHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImg: ShapeableImageView = view.findViewById(R.id.personImageView)
        val farmerName: TextView = view.findViewById(R.id.senderNameTextView)
        val deliveryIdText: TextView = view.findViewById(R.id.messageContentTextView)
        val dateOnly: TextView = view.findViewById(R.id.messageTimestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val arrivalDateOnly = item.deliveryArrivalTime?.toDate()?.let { sdf.format(it) } ?: "Unknown"

        val name = farmerNames[item.deliveryId] ?: "Farmer"
        holder.farmerName.text = name
        holder.deliveryIdText.text = "Delivery ID: ${item.deliveryId}"
        holder.dateOnly.text = arrivalDateOnly

        holder.itemView.setOnClickListener {
            onItemClick(item.deliveryId)
        }

        val imageUrl = farmerImages[item.deliveryId]
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.profileImg.context)
                .load(imageUrl)
                .placeholder(R.drawable.profile_circle)
                .into(holder.profileImg)
        } else {
            holder.profileImg.setImageResource(R.drawable.profile_circle)
        }
    }

    override fun getItemCount(): Int = items.size

}
