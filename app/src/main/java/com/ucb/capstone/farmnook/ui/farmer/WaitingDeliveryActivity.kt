package com.ucb.capstone.farmnook.ui.farmer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.utils.loadMapInWebView

class WaitingDeliveryActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loadingLayout: View
    private lateinit var confirmationLayout: View
    private lateinit var deliveryRequestRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_delivery)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }



        loadingLayout = findViewById(R.id.loadingLayout)
        confirmationLayout = findViewById(R.id.confirmationLayout)

        val deliveryReqId = intent.getStringExtra("requestId") ?: ""

        if (deliveryReqId.isEmpty()) {
            Toast.makeText(this, "No Request ID received!", Toast.LENGTH_SHORT).show()
            finish()  // Close the activity if no ID is passed
            return
        }
        deliveryRequestRef = FirebaseFirestore.getInstance()
            .collection("deliveryRequests")
            .document(deliveryReqId)

        fetchPickupAndDestination(deliveryReqId)
        listenForDeliveryConfirmation()
    }


    private fun fetchPickupAndDestination(requestId: String) {
        val db = FirebaseFirestore.getInstance()
        val requestRef = db.collection("deliveryRequests").document(requestId)

        requestRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val pickup = document.getString("pickupLocation") ?: ""
                val drop = document.getString("destinationLocation") ?: ""
                val webView = findViewById<android.webkit.WebView>(R.id.mapView)

                if (pickup.isNotEmpty() && drop.isNotEmpty()) {
                    loadMapInWebView(webView, pickup, drop)
                } else {
                    Toast.makeText(this, "Pickup or destination location is missing.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Request data not found!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load delivery request data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForDeliveryConfirmation() {
        deliveryRequestRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Failed to listen for updates.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val isAccepted = snapshot.getBoolean("isAccepted") ?: false
                if (isAccepted) {
                    showConfirmationLayout()
                } else {
                    showLoadingLayout()
                }
            }
        }
    }

    private fun showConfirmationLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
    }

    private fun showLoadingLayout() {
        loadingLayout.visibility = View.VISIBLE
        confirmationLayout.visibility = View.GONE
    }

}