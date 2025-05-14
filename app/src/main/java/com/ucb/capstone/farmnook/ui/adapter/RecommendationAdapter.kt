package com.ucb.capstone.farmnook.ui.adapter

import android.annotation.SuppressLint
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
    private val onAvailableClicked: (VehicleWithBusiness) -> Unit,
    var filterMode: Int
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val businessName: TextView = view.findViewById(R.id.business_name)
        val vehicle: TextView = view.findViewById(R.id.vehicle)
        val ratings: TextView = view.findViewById(R.id.ratings)
        val cost: TextView = view.findViewById(R.id.costEstimation)
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val availableBtn: Button = view.findViewById(R.id.available_button)
        val kmAway: TextView = view.findViewById(R.id.kmAway)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vehicle_recommend_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = vehicles.size

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = vehicles[position]
        holder.businessName.text = item.businessName
        holder.vehicle.text = item.vehicleType

        holder.profileImage.loadImage(item.profileImage)

        holder.kmAway.text = item.pickupDistanceKm?.let {
            "${String.format("%.1f", it)} km away"
        } ?: ""
        holder.ratings.text = String.format("%.1f", item.averageRating ?: 0.0)


        // Rating display
        holder.ratings.text = "${String.format("%.1f", item.averageRating ?: 0.0)}"
        holder.ratings.visibility = if (filterMode == 0 || filterMode == 3) View.VISIBLE else View.GONE

        // kmAway display
        val pickupKm = item.pickupDistanceKm
        val formattedKm = if (pickupKm != null) "${"%.1f".format(pickupKm)} km away" else "N/A"
        holder.kmAway.text = formattedKm
        holder.kmAway.visibility = if (filterMode == 0 || filterMode == 2) View.VISIBLE else View.GONE

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