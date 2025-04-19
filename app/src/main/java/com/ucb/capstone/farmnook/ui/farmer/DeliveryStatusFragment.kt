package com.ucb.capstone.farmnook.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.farmer.add_delivery.RateDelivery

class DeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var farmerId: String = ""
    private var haulerAdminId: String = ""
    private val deliveryId: String = "" // Replace with actual delivery ID when needed

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom sheet setup
        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false

        val btnRateDelivery = view.findViewById<Button>(R.id.btnRateDelivery)
        btnRateDelivery.isEnabled = false // Disable until IDs are fetched

        // Get the logged-in farmer's UID (assuming Firebase Auth is used)
        auth.currentUser?.uid?.let { uid ->
            farmerId = uid
        }

        // Fetch Hauler Business Admin userId from Firestore
        db.collection("users")
            .whereEqualTo("userType", "Hauler Business Admin")
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                haulerAdminId = doc?.getString("userId") ?: ""

                if (farmerId.isNotBlank() && haulerAdminId.isNotBlank()) {
                    btnRateDelivery.isEnabled = true
                    btnRateDelivery.setOnClickListener {
                        navigateToRateDelivery(deliveryId, farmerId, haulerAdminId)
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                // TODO: Handle error (e.g., show a message to the user)
            }
    }

    private fun navigateToRateDelivery(deliveryId: String, farmerId: String, haulerId: String) {
        val intent = Intent(requireContext(), RateDelivery::class.java).apply {
            putExtra("deliveryId", deliveryId)
            putExtra("farmerId", farmerId)
            putExtra("haulerId", haulerId)
        }
        startActivity(intent)
    }
}
