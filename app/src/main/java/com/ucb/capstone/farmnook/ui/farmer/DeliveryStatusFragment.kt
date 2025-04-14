package com.ucb.capstone.farmnook.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.farmer.add_delivery.RateDelivery


class DeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false

        val btnRateDelivery = view.findViewById<Button>(R.id.btnRateDelivery)

        // Replace with actual Hauler Business Admin ID if known
        val deliveryId = ""
        val farmerId = "P0cgv2RFO6Mob6ptn9bFIM2LlRE3"
        val haulerBusinessAdminId = "DhDTGRxOENMtM0SmemtFJ1926X53" // e.g., from Firestore or passed in

        btnRateDelivery.setOnClickListener {
            navigateToRateDelivery(deliveryId, farmerId, haulerBusinessAdminId)
        }
    }

    private fun navigateToRateDelivery(deliveryId: String, farmerId: String, haulerBusinessAdminId: String) {
        val intent = Intent(requireContext(), RateDelivery::class.java).apply {
            putExtra("deliveryId", deliveryId)
            putExtra("farmerId", farmerId)
            putExtra("haulerId", haulerBusinessAdminId)
        }
        startActivity(intent)
    }
}
