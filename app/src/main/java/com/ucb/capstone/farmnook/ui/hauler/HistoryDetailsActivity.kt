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
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import java.text.SimpleDateFormat
import java.util.Locale
import android.location.Geocoder
import com.ucb.capstone.farmnook.utils.loadImage

class HistoryDetailsActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetTextI18n", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_details)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        val pickup = intent.getStringExtra("pickupAddress") ?: "Unknown"
        val drop = intent.getStringExtra("destinationAddress") ?: "Unknown"
        val geocoder = Geocoder(this, Locale.getDefault())
        val pickupAddress = getAddressFromLatLng(pickup, geocoder)
        val dropAddress = getAddressFromLatLng(drop, geocoder)
        val farmerName = intent.getStringExtra("farmerName") ?: "Unknown"
        val farmerProfileImg = intent.getStringExtra("profileImg")
        val profileImageView = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
        profileImageView.loadImage(farmerProfileImg)

        // Set readable addresses (from Intent)
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = dropAddress
        findViewById<TextView>(R.id.farmerName).text = farmerName

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        val deliveryId = intent.getStringExtra("deliveryId") ?: return
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: ""
        fetchDeliveryDetails(deliveryId, pickup, drop, estimatedTime, farmerName)

    }
    private fun fetchDeliveryDetails(
        deliveryId: String,
        pickup: String,
        drop: String,
        estimatedTime: String,
        farmerName: String
    ) {
        val historyQuery = FirebaseFirestore.getInstance().collection("deliveryHistory")
            .whereEqualTo("deliveryId", deliveryId)
            .limit(1)

        historyQuery.addSnapshotListener { historyDocs, error ->
            if (error != null || historyDocs == null || historyDocs.isEmpty) return@addSnapshotListener

            val history = historyDocs.documents[0]
            val completedAt = history.getTimestamp("deliveryArrivalTime")?.toDate()
            val formattedTime = completedAt?.let {
                SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(it)
            } ?: "N/A"

            findViewById<TextView>(R.id.dateTime).text = formattedTime

            val deliveryRef = FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
            deliveryRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val requestId = doc.getString("requestId") ?: return@addOnSuccessListener

                val requestRef = FirebaseFirestore.getInstance().collection("deliveryRequests").document(requestId)
                requestRef.addSnapshotListener { req, reqError ->
                    if (reqError != null || req == null || !req.exists()) return@addSnapshotListener

                    val weight = req.get("weight").toString()
                    val productType = req.getString("productType") ?: "Unknown"
                    val purpose = req.getString("purpose") ?: "General"
                    val vehicleId = req.getString("vehicleId") ?: ""
                    val estimatedCost = req.getDouble("estimatedCost") ?: 0.0

                    findViewById<TextView>(R.id.weightAmount).text = "$weight kg"
                    findViewById<TextView>(R.id.productType).text = "$purpose\n$productType"
                    findViewById<TextView>(R.id.totalCost).text = "₱${kotlin.math.ceil(estimatedCost).toInt()}"

                    findViewById<TextView>(R.id.farmerName).text = farmerName
                    findViewById<TextView>(R.id.estimatedTime).text = estimatedTime

                    if (vehicleId.isNotEmpty()) {
                        FirebaseFirestore.getInstance().collection("vehicles")
                            .document(vehicleId)
                            .get()
                            .addOnSuccessListener { vehicleDoc ->
                                if (vehicleDoc.exists()) {
                                    val model = vehicleDoc.getString("model") ?: "Unknown"
                                    val vehicleType = vehicleDoc.getString("vehicleType") ?: "Unknown"
                                    findViewById<TextView>(R.id.vehicle).text = "$model\n$vehicleType"
                                }
                            }
                    }

                    val webView = findViewById<WebView>(R.id.mapView)
                    loadMapInWebView(webView, pickup, drop)
                }
            }
        }
    }
}
