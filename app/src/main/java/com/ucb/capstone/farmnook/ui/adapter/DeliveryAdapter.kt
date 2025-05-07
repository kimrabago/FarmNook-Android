package com.ucb.capstone.farmnook.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryItem
import com.ucb.capstone.farmnook.ui.hauler.DeliveryDetailsActivity
///WAY GAMIT
class DeliveryAdapter(private val deliveryList: List<DeliveryItem>) :
    RecyclerView.Adapter<DeliveryAdapter.DeliveryViewHolder>() {

    class DeliveryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pickupLocation: TextView = view.findViewById(R.id.pickUpLocation)
        val provincePickup: TextView = view.findViewById(R.id.provincePickup)
        val destination: TextView = view.findViewById(R.id.destination)
        val provinceDestination: TextView = view.findViewById(R.id.provinceDestination)
        val estimatedTime: TextView = view.findViewById(R.id.estimatedTime)
        val totalCost: TextView = view.findViewById(R.id.totalCost)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val viewDeliverBtn: Button = view.findViewById(R.id.viewDeliverBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery, parent, false)
        return DeliveryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {
        val item = deliveryList[position]

        // Avoid null values by using default placeholders
        holder.pickupLocation.text = item.pickupLocation ?: "Unknown Pickup"
        holder.provincePickup.text = item.provincePickup ?: "Unknown Province"
        holder.destination.text = item.destination ?: "Unknown Destination"
        holder.provinceDestination.text = item.provinceDestination ?: "Unknown Province"
        holder.estimatedTime.text = item.estimatedTime ?: "ETA: Not Available"
        holder.totalCost.text = item.totalCost ?: "Cost: Not Available"

        // Load profile image using Glide (ensure Glide is added in build.gradle)
        if (!item.profileImage.isNullOrEmpty()) {
            Glide.with(holder.profileImage.context)
                .load(item.profileImage)
                .placeholder(R.drawable.profile_circle) // Placeholder while loading
                .error(R.drawable.profile_white) // Error image if loading fails
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_circle) // Set default if no image
        }

        // Handle button click properly
        holder.viewDeliverBtn.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeliveryDetailsActivity::class.java)
            intent.putExtra("deliveryId", item.id) // Pass delivery ID to next screen
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = deliveryList.size
}
