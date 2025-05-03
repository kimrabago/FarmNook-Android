package com.ucb.capstone.farmnook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.utils.loadImage
import kotlin.math.round

class RecommendationAdapter(
    private val vehicles: List<VehicleWithBusiness>,
    private val onAvailableClicked: (VehicleWithBusiness) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val businessName: TextView = view.findViewById(R.id.business_name)
        val vehicle: TextView = view.findViewById(R.id.vehicle)
        val ratings: TextView = view.findViewById(R.id.ratings)
        val cost: TextView = view.findViewById(R.id.costEstimation)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val availableBtn: Button = view.findViewById(R.id.available_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vehicle_recommend_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = vehicles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = vehicles[position]
        holder.businessName.text = item.businessName
        holder.vehicle.text = item.vehicleType

        holder.profileImage.loadImage(item.profileImage)

        holder.ratings.text = "${String.format("%.1f", item.averageRating ?: 0.0)}"

        // 👇 Show estimated cost if available
        val estimatedCost = item.estimatedCost
        holder.cost.text = if (estimatedCost != null)
            "₱%.0f".format(round(estimatedCost))
        else
            "Estimating..."

        holder.availableBtn.setOnClickListener {
            onAvailableClicked(item)
        }
    }
}