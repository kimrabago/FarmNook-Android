package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness

class DeliverySummaryDialogFragment : DialogFragment() {

    private lateinit var vehicle: VehicleWithBusiness
    private lateinit var delivery: DeliveryRequest
    private var onHireConfirmed: ((Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit)? = null

    companion object {
        fun newInstance(
            vehicle: VehicleWithBusiness,
            delivery: DeliveryRequest,
            onHire: (Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit
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
                Vehicle: ${vehicle.vehicleType} - ${vehicle.model}
                Plate Number: ${vehicle.plateNumber}
                
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
