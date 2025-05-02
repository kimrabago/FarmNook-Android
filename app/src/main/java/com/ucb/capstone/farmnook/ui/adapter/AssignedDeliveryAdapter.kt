package com.ucb.capstone.farmnook.ui.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryDisplayItem

class AssignedDeliveryAdapter(
    private var deliveries: List<DeliveryDisplayItem>,
    private val onViewClick: (DeliveryDisplayItem) -> Unit
) : RecyclerView.Adapter<AssignedDeliveryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pickupLocation: TextView = view.findViewById(R.id.pickUpLocation)
        val provincePickup: TextView = view.findViewById(R.id.provincePickup)
        val destination: TextView = view.findViewById(R.id.destination)
        val provinceDestination: TextView = view.findViewById(R.id.provinceDestination)
        val estimatedTime: TextView = view.findViewById(R.id.estimatedTime)
        val totalCost: TextView = view.findViewById(R.id.totalCost)
        val viewBtn: View = view.findViewById(R.id.viewDeliverBtn)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val delivery = deliveries[position]
        holder.pickupLocation.text = "Pickup Location"
        holder.provincePickup.text = delivery.pickupLocation
        holder.destination.text = "Destination Location"
        holder.provinceDestination.text = delivery.destinationLocation
        holder.estimatedTime.text = delivery.estimatedTime
        holder.totalCost.text = "₱${delivery.totalCost}"


        holder.viewBtn.setOnClickListener {
            Log.d("DeliveryAdapter", "View button clicked for ${delivery.deliveryId}")
            onViewClick(delivery)
        }
    }

    override fun getItemCount(): Int = deliveries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery, parent, false)
        return ViewHolder(view)
    }

}

