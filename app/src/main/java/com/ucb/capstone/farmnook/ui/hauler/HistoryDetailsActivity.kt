package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.util.getAddressFromLatLng
import java.text.SimpleDateFormat
import java.util.Locale
import android.location.Geocoder

class HistoryDetailsActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_details)

        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false

        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        val deliveryId = intent.getStringExtra("deliveryId") ?: return

        val historyQuery = FirebaseFirestore.getInstance().collection("deliveryHistory")
            .whereEqualTo("deliveryId", deliveryId)
            .limit(1)

        historyQuery.get().addOnSuccessListener { historyDocs ->
            if (!historyDocs.isEmpty) {
                val history = historyDocs.documents[0]
                val completedAt = history.getTimestamp("deliveryArrivalTime")?.toDate()
                val formattedTime = completedAt?.let {
                    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(it)
                } ?: "N/A"

                // ✅ Set date and time
                findViewById<TextView>(R.id.dateTime).text = formattedTime

                val deliveryRef =
                    FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
                deliveryRef.get().addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val requestId = doc.getString("requestId") ?: return@addOnSuccessListener

                        // Set date and time
                        findViewById<TextView>(R.id.dateTime).text = formattedTime

                        // Fetch deliveryRequest details
                        val requestRef =
                            FirebaseFirestore.getInstance().collection("deliveryRequests")
                                .document(requestId)
                        requestRef.get().addOnSuccessListener { req ->
                            val pickup = req.getString("pickupLocation") ?: ""
                            val drop = req.getString("destinationLocation") ?: ""
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val pickupAddress = getAddressFromLatLng(pickup, geocoder)
                            val dropAddress = getAddressFromLatLng(drop, geocoder)
                            val weight = req.get("weight").toString()
                            val productType = req.getString("productType") ?: "Unknown"
                            val purpose = req.getString("purpose") ?: "General"
                            val vehicleId = req.getString("vehicleId") ?: ""

                            // Set readable addresses (from Intent)
                            findViewById<TextView>(R.id.provincePickup).text = pickupAddress
                            findViewById<TextView>(R.id.provinceDestination).text =
                                dropAddress
                            findViewById<TextView>(R.id.weightAmount).text = "$weight kg"
                            findViewById<TextView>(R.id.productType).text = "$purpose\n$productType"

                            // Load vehicle model + type
                            if (vehicleId.isNotEmpty()) {
                                FirebaseFirestore.getInstance().collection("vehicles")
                                    .document(vehicleId)
                                    .get()
                                    .addOnSuccessListener { vehicleDoc ->
                                        if (vehicleDoc.exists()) {
                                            val model = vehicleDoc.getString("model") ?: "Unknown"
                                            val vehicleType =
                                                vehicleDoc.getString("vehicleType") ?: "Unknown"
                                            findViewById<TextView>(R.id.vehicle).text =
                                                "$model\n$vehicleType"
                                        }
                                    }
                            }

                            // Load map using raw coordinates
                            val encodedPickup = pickup.replace(" ", "")
                            val encodedDrop = drop.replace(" ", "")
                            val mapUrl =
                                "https://farmnook-web.vercel.app/map-viewer?pickup=$encodedPickup&drop=$encodedDrop"

                            val webView = findViewById<WebView>(R.id.mapView)
                            webView.settings.javaScriptEnabled = true
                            webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                            webView.settings.domStorageEnabled = true
                            webView.webViewClient = android.webkit.WebViewClient()
                            webView.loadUrl(mapUrl)
                            findViewById<WebView>(R.id.mapView).loadUrl(mapUrl)
                        }
                    }
                }
            }
        }
    }
}
