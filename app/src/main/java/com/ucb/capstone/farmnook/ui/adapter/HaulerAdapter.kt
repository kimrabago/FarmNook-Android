package com.ucb.capstone.farmnook.ui.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Hauler
import de.hdodenhof.circleimageview.CircleImageView
//WANAY GAMIT
class HaulerAdapter(
    private val context: Context,
    private val haulerList: List<Hauler>
) : RecyclerView.Adapter<HaulerAdapter.HaulerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HaulerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vehicle_recommend_item, parent, false)
        return HaulerViewHolder(view)
    }

    override fun onBindViewHolder(holder: HaulerViewHolder, position: Int) {
        val hauler = haulerList[position]

        // Bind hauler data to UI elements in the RecyclerView item
        holder.businessName.text = hauler.name
//        holder.vehicle.text = hauler.rating
        holder.vehicleWeight.text = hauler.price

        // Availability logic
        if (hauler.isAvailable) {
            holder.statusButton.text = "Available"
            holder.statusButton.setBackgroundResource(R.color.yellow_green)
        } else {
            holder.statusButton.text = "Not Available"
            holder.statusButton.setBackgroundResource(R.color.red)
        }

        // Open the dialog with driver details when clicking the item
        holder.itemView.setOnClickListener {
            showHaulerDialog(hauler)
        }
    }

    override fun getItemCount(): Int = haulerList.size

    class HaulerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        val businessName: TextView = itemView.findViewById(R.id.business_name)
        val vehicle: TextView = itemView.findViewById(R.id.vehicle)
        val vehicleWeight: TextView = itemView.findViewById(R.id.costEstimation)
        val statusButton: Button = itemView.findViewById(R.id.available_button)
    }

    // Function to show the driver details dialog
    private fun showHaulerDialog(hauler: Hauler) {
        val dialog = Dialog(context)

        // Remove default title bar from dialog
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Set the custom layout for this dialog

        // Make the dialog background transparent so your rounded corners show properly
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // OPTIONAL: Match entire screen width
        // dialog.window?.setLayout(
        //     ViewGroup.LayoutParams.MATCH_PARENT,
        //     ViewGroup.LayoutParams.WRAP_CONTENT
        // )

        // CURRENT: Use about 90% of the screen width
        val screenWidth = context.resources.displayMetrics.widthPixels
        dialog.window?.setLayout(
            (screenWidth * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Now bind your views in the dialog layout
        val profileImage = dialog.findViewById<CircleImageView>(R.id.profileImage)
        val haulerName = dialog.findViewById<TextView>(R.id.haulerName)
        val plateNumber = dialog.findViewById<TextView>(R.id.plateNumber)
        val location = dialog.findViewById<TextView>(R.id.location)
        val vehicleType = dialog.findViewById<TextView>(R.id.vehicleType)
        val model = dialog.findViewById<TextView>(R.id.model)
        val capacity = dialog.findViewById<TextView>(R.id.capacity)
        val closeDialog = dialog.findViewById<ImageView>(R.id.closeDialog)

        // Populate the dialog with driver details from the hauler object
        haulerName.text = hauler.name
        plateNumber.text = hauler.plateNumber
        location.text = hauler.location
        vehicleType.text = hauler.vehicleType
        model.text = hauler.model
        capacity.text = hauler.capacity



        // Dismiss the dialog when the close button is clicked
        closeDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
