package com.ucb.capstone.farmnook.ui.farmer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.utils.loadMapInWebView

class FarmerDeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loadingLayout: View
    private lateinit var confirmationLayout: View
    private lateinit var noActiveDeliveryLayout: View
    private lateinit var deliveryRequestRef: DocumentReference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Use view.findViewById() because you're in Fragment
        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        loadingLayout = view.findViewById(R.id.loadingLayout)
        confirmationLayout = view.findViewById(R.id.confirmationLayout)
        noActiveDeliveryLayout = view.findViewById(R.id.noActiveDeliveryLayout)

        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE

        // ✅ Use arguments instead of intent
        val deliveryReqId = arguments?.getString("requestId") ?: ""
        Log.d("DeliveryStatusFragment", "Received requestId: $deliveryReqId")

        if (deliveryReqId.isEmpty()) {
            showNoActiveDelivery(view)
            return
        }

        deliveryRequestRef = FirebaseFirestore.getInstance()
            .collection("deliveryRequests")
            .document(deliveryReqId)

        fetchPickupAndDestination(deliveryReqId, view)
        listenForDeliveryConfirmation()

        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener {
            cancelDeliveryRequest()
        }
    }

    private fun showNoActiveDelivery(view: View) {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE

        val webView = view.findViewById<WebView>(R.id.mapView)
        // Load a blank or gray page
        webView.setBackgroundColor(Color.parseColor("#E0E0E0"))  // Gray color
        webView.loadData(
            "<html><body style='background-color:#E0E0E0;'><h3 style='color:#888; text-align:center; margin-top:50%;'>No Active Delivery</h3></body></html>",
            "text/html",
            "utf-8"
        )
    }

    private fun fetchPickupAndDestination(requestId: String, view: View) {
        val db = FirebaseFirestore.getInstance()
        val requestRef = db.collection("deliveryRequests").document(requestId)

        requestRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val pickup = document.getString("pickupLocation") ?: ""
                val drop = document.getString("destinationLocation") ?: ""
                val webView = view.findViewById<WebView>(R.id.mapView)

                if (pickup.isNotEmpty() && drop.isNotEmpty()) {
                    loadMapInWebView(webView, pickup, drop)
                } else {
                    Toast.makeText(requireContext(), "Pickup or destination location is missing.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Request data not found!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load delivery request data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForDeliveryConfirmation() {
        deliveryRequestRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(requireContext(), "Failed to listen for updates.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val isAccepted = snapshot.getBoolean("isAccepted") ?: false
                if (isAccepted) {
                    fetchDeliveryDetails(snapshot.id)
                } else {
                    showLoadingLayout()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchDeliveryDetails(requestId: String) {
        val deliveriesRef = FirebaseFirestore.getInstance()
            .collection("deliveries")
            .whereEqualTo("requestId", requestId)

        deliveriesRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val deliveryDoc = querySnapshot.documents[0]
                val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener
                val deliveryId = deliveryDoc.getString("deliveryId") ?: return@addOnSuccessListener

                val deliveryIdTextView = view?.findViewById<TextView>(R.id.deliveryId)
                deliveryIdTextView?.text = "Delivery ID: $deliveryId"

                // 🟢 Fetch the vehicleId from the deliveryRequest document first:
                deliveryRequestRef.get().addOnSuccessListener { requestDoc ->
                    val vehicleId = requestDoc.getString("vehicleId") ?: return@addOnSuccessListener

                    fetchVehicleDetails(vehicleId)  // ✅ Fetch vehicle info here!
                    fetchHaulerDetails(haulerId)    // ✅ Already existing hauler fetching logic
                }
            } else {
                Log.w("DeliveryStatusFragment", "No delivery found for requestId: $requestId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching delivery details: ${e.message}")
        }
    }

    private fun fetchHaulerDetails(haulerId: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(haulerId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val haulerID = document.getString("userId")
                val haulerName = document.getString("firstName") + " " + document.getString("lastName")
                val haulerProfileImg = document.getString("profileImageUrl")

                val haulerIdTextView = view?.findViewById<TextView>(R.id.haulerId)
                val haulerNameTextView = view?.findViewById<TextView>(R.id.haulerName)
                val haulerProfileImage = view?.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)

                haulerIdTextView?.text = haulerID
                haulerNameTextView?.text = haulerName

                if (!haulerProfileImg.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(haulerProfileImg)
                        .placeholder(R.drawable.profile_circle)
                        .error(R.drawable.profile_circle)
                        .into(haulerProfileImage!!)
                } else {
                    haulerProfileImage?.setImageResource(R.drawable.profile_circle)
                }

                showConfirmationLayout()
            } else {
                Log.w("DeliveryStatusFragment", "Hauler not found with ID: $haulerId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching hauler details: ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchVehicleDetails(vehicleId: String) {
        val vehicleRef = FirebaseFirestore.getInstance()
            .collection("vehicles")
            .document(vehicleId)

        vehicleRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val vehicleType = document.getString("vehicleType") ?: "Unknown Type"
                val model = document.getString("model") ?: "Unknown Model"
                val plateNumber = document.getString("plateNumber") ?: "Unknown Plate"

                val vehicleTypeTextView = view?.findViewById<TextView>(R.id.vehicleType)
                val plateNumberTextView = view?.findViewById<TextView>(R.id.plateNumber)

                vehicleTypeTextView?.text =  "$vehicleType - $model"
                plateNumberTextView?.text =  plateNumber
            } else {
                Log.w("DeliveryStatusFragment", "Vehicle not found with ID: $vehicleId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching vehicle details: ${e.message}")
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

    private fun cancelDeliveryRequest() {
        deliveryRequestRef.update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Delivery request cancelled successfully.", Toast.LENGTH_SHORT).show()

                val navBar = activity as? NavigationBar
                navBar?.let { nav ->
                    nav.restoreActiveRequestId {
                        requireActivity().runOnUiThread {
                            nav.resetToDashboard()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to cancel delivery request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}