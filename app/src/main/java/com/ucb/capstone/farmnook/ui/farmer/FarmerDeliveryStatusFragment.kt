package com.ucb.capstone.farmnook.ui.farmer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.*
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.menu.NavigationBar

class FarmerDeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loadingLayout: View
    private lateinit var confirmationLayout: View
    private lateinit var haulerToPickupLayout: View
    private lateinit var haulerArrivalLayout: View
    private lateinit var noActiveDeliveryLayout: View
    private lateinit var webView: WebView

    private lateinit var deliveryRequestRef: DocumentReference
    private var deliveryId: String? = null
    private var listenerRegistration: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        webView = view.findViewById(R.id.mapView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = WebViewClient()

        loadingLayout = view.findViewById(R.id.loadingLayout)
        confirmationLayout = view.findViewById(R.id.confirmationLayout)
        haulerToPickupLayout = view.findViewById(R.id.haulerToPickupLayout)
        haulerArrivalLayout = view.findViewById(R.id.haulerArrival)
        noActiveDeliveryLayout = view.findViewById(R.id.noActiveDeliveryLayout)

        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener { cancelDeliveryRequest() }

        val requestId = arguments?.getString("requestId") ?: ""
        if (requestId.isEmpty()) {
            showNoActiveDelivery()
            return
        }

        deliveryRequestRef = FirebaseFirestore.getInstance().collection("deliveryRequests").document(requestId)

        deliveryRequestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                showNoActiveDelivery()
                return@addOnSuccessListener
            }

            val pickup = doc.getString("pickupLocation") ?: ""
            val destination = doc.getString("destinationLocation") ?: ""

            if (pickup.isNotEmpty() && destination.isNotEmpty()) {
                fetchDeliveryIdAndLoadStatus(doc.id, pickup, destination)
            } else {
                Toast.makeText(requireContext(), "Missing pickup/destination", Toast.LENGTH_SHORT).show()
                showNoActiveDelivery()
            }
        }
    }

    private fun fetchDeliveryIdAndLoadStatus(requestId: String, pickup: String, destination: String) {
        FirebaseFirestore.getInstance().collection("deliveries")
            .whereEqualTo("requestId", requestId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showLoadingLayout()
                    return@addOnSuccessListener
                }

                val deliveryDoc = querySnapshot.documents[0]
                deliveryId = deliveryDoc.id
                val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                val encodedPickup = pickup.replace(" ", "")
                val encodedDrop = destination.replace(" ", "")
                val mapUrl = "https://farmnook-web.vercel.app/live-tracking?pickup=$encodedPickup&drop=$encodedDrop&haulerId=$haulerId"

                Log.d("MAP_DEBUG", "🌍 Loading map URL: $mapUrl")
                webView.loadUrl(mapUrl)

                listenToDeliveryStatus(deliveryId!!)
                fetchHaulerDetails(haulerId)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get delivery details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToDeliveryStatus(deliveryId: String) {
        val deliveryRef = FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
        listenerRegistration = deliveryRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val isDone = snapshot.getBoolean("isDone") ?: false
            val arrivedAtDestination = snapshot.getBoolean("arrivedAtDestination") ?: false
            val arrivedAtPickup = snapshot.getBoolean("arrivedAtPickup") ?: false

            when {
                isDone -> showCompletedMessage()
                arrivedAtDestination -> showArrivalLayout()
                arrivedAtPickup -> showEnRouteLayout()
                else -> showConfirmationLayout()
            }
        }
    }

    private fun showLoadingLayout() {
        loadingLayout.visibility = View.VISIBLE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
    }

    private fun showConfirmationLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
    }

    private fun showEnRouteLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.VISIBLE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
    }

    private fun showArrivalLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.VISIBLE
        noActiveDeliveryLayout.visibility = View.GONE
    }

    private fun showCompletedMessage() {
        Toast.makeText(requireContext(), "✅ Delivery completed!", Toast.LENGTH_LONG).show()
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE
    }

    private fun showNoActiveDelivery() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE

        // Only show this gray fallback when no delivery exists
        webView.setBackgroundColor(Color.parseColor("#E0E0E0"))
        webView.loadData(
            "<html><body style='background-color:#E0E0E0;'><h3 style='color:#888; text-align:center; margin-top:50%;'>No Active Delivery</h3></body></html>",
            "text/html", "utf-8"
        )
    }

    private fun fetchHaulerDetails(haulerId: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(haulerId)
        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) return@addOnSuccessListener

            val haulerName = "${doc.getString("firstName") ?: ""} ${doc.getString("lastName") ?: ""}"
            val profileUrl = doc.getString("profileImageUrl")

            view?.findViewById<TextView>(R.id.haulerName)?.text = haulerName
            val profileImg = view?.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)

            if (!profileUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(profileUrl)
                    .placeholder(R.drawable.profile_circle)
                    .error(R.drawable.profile_circle)
                    .into(profileImg!!)
            } else {
                profileImg?.setImageResource(R.drawable.profile_circle)
            }
        }
    }

    private fun cancelDeliveryRequest() {
        deliveryRequestRef.update("status", "Cancelled").addOnSuccessListener {
            Toast.makeText(requireContext(), "Cancelled successfully", Toast.LENGTH_SHORT).show()
            (activity as? NavigationBar)?.resetToDashboard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}
