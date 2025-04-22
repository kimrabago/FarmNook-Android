package com.ucb.capstone.farmnook.ui.hauler.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R

class DeliveryStatusFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivery_status, container, false)

        Log.d("DeliveryStatus", "Fragment created.")

        val pickup = arguments?.getString("pickup") ?: return view
        val destination = arguments?.getString("destination") ?: return view
        val pickupAddress = arguments?.getString("pickupAddress") ?: "Unknown"
        val destinationAddress = arguments?.getString("destinationAddress") ?: "Unknown"
        val deliveryId = arguments?.getString("deliveryId") ?: return view

        val fromAddress = view.findViewById<TextView>(R.id.from_address)
        val toAddress = view.findViewById<TextView>(R.id.to_address)
        fromAddress.text = pickupAddress
        toAddress.text = destinationAddress

        // ✅ Setup BottomSheet Behavior (Lock sliding)
        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.isDraggable = false // ✅ Lock the sliding behavior

        Log.d("DeliveryStatus", "Bottom sheet behavior initialized and locked.")

        // ✅ Setup WebView
        webView = view.findViewById(R.id.mapView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.webViewClient = WebViewClient()

        // ✅ Fetch haulerAssignedId for the map
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("deliveries").document(deliveryId).get()
            .addOnSuccessListener { document ->
                val haulerId = document.getString("haulerAssignedId") ?: "anonymous"
                Log.d("DeliveryStatus", "Fetched haulerAssignedId: $haulerId")

                val mapUrl = "https://farmnook-web.vercel.app/live-tracking?pickup=${pickup.replace(" ", "")}&drop=${destination.replace(" ", "")}&haulerId=$haulerId"

                Log.d("WebViewStatus", "Loading live-tracking map URL: $mapUrl")
                webView.loadUrl(mapUrl)
            }
            .addOnFailureListener { e ->
                Log.e("DeliveryStatus", "Failed to fetch haulerAssignedId: ${e.message}")
            }

        return view
    }
}
