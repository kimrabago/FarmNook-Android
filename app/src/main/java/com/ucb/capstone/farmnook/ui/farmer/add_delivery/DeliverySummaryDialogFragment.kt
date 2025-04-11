package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ucb.capstone.farmnook.data.model.Delivery
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness

class DeliverySummaryDialogFragment : DialogFragment() {

    private lateinit var vehicle: VehicleWithBusiness
    private lateinit var delivery: Delivery
    private var onHireConfirmed: ((Pair<VehicleWithBusiness, Delivery>) -> Unit)? = null

    companion object {
        fun newInstance(
            vehicle: VehicleWithBusiness,
            delivery: Delivery,
            onHire: (Pair<VehicleWithBusiness, Delivery>) -> Unit
        ): DeliverySummaryDialogFragment {
            val fragment = DeliverySummaryDialogFragment()
            fragment.vehicle = vehicle
            fragment.delivery = delivery
            fragment.onHireConfirmed = onHire
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Confirm Hire")
            .setMessage(
                """
                Business: ${vehicle.businessName}
                Vehicle: ${vehicle.model} (${vehicle.size})
                Plate Number: ${vehicle.plateNumber}
                Max Weight: ${vehicle.maxWeightKg} kg
                
                Pickup: ${delivery.pickupLocation}
                Destination: ${delivery.destinationLocation}
                Purpose: ${delivery.purpose}
                Product Type: ${delivery.productType}
                Weight: ${delivery.weight} kg
                
                Proceed with delivery request?
                """.trimIndent()
            )
            .setPositiveButton("Hire") { _, _ ->
                onHireConfirmed?.invoke(vehicle to delivery)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
