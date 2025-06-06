package com.ucb.capstone.farmnook.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.utils.loadImage
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
        val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
        val arrivalDateOnly = item.deliveryArrivalTime?.toDate()?.let { sdf.format(it) } ?: "Unknown"

        val name = farmerNames[item.deliveryId] ?: "Farmer"
        holder.farmerName.text = name
        holder.deliveryIdText.text = "DRef: ${item.deliveryId}"
        holder.dateOnly.text = arrivalDateOnly

        holder.itemView.setOnClickListener {
            onItemClick(item.deliveryId)
        }

        val imageUrl = farmerImages[item.deliveryId]
        holder.profileImg.loadImage(imageUrl)

    }
    override fun getItemCount(): Int = items.size
}
