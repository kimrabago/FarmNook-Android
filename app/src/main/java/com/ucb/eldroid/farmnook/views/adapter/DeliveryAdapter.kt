package com.ucb.eldroid.farmnook.views.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.DeliveryItem
import com.ucb.eldroid.farmnook.views.hauler.DeliveryDetailsActivity

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
        val viewDeliverBtn: Button = view.findViewById(R.id.viewDeliverBtn)// Assuming you have an ImageView for the profile image
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery, parent, false)



        return DeliveryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeliveryViewHolder, position: Int) {

        val item = deliveryList[position]

        holder.pickupLocation.text = item.pickupLocation
        holder.provincePickup.text = item.provincePickup
        holder.destination.text = item.destination
        holder.provinceDestination.text = item.provinceDestination
        holder.estimatedTime.text = item.estimatedTime
        holder.totalCost.text = item.totalCost
        // Load profile image (you might want to use Glide or Picasso for image loading)
        // Glide.with(holder.profileImage.context).load(item.profileImage).into(holder.profileImage)

        // Handle button click properly
        holder.viewDeliverBtn.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeliveryDetailsActivity::class.java)
            intent.putExtra("deliveryId", item.id) // Pass delivery ID to the next screen
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = deliveryList.size
}