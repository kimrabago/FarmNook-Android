package com.ucb.capstone.farmnook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness

class RecommendationAdapter(
    private val vehicles: List<VehicleWithBusiness>,
    private val onAvailableClicked: (VehicleWithBusiness) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val businessName: TextView = view.findViewById(R.id.business_name)
        val modelName: TextView = view.findViewById(R.id.vehicle_model)
        val weight: TextView = view.findViewById(R.id.vehicle_weight)
        val availableBtn: Button = view.findViewById(R.id.available_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hauler_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = vehicles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = vehicles[position]
        holder.businessName.text = item.businessName
        holder.modelName.text = "Model: ${item.model}"
        holder.weight.text = "Max Weight: ${item.maxWeightKg} kg"

        holder.availableBtn.setOnClickListener {
            onAvailableClicked(item)
        }
    }
}