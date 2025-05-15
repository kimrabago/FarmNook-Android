package com.ucb.capstone.farmnook.ui.users

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
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
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
        val pickupAddress = intent.getStringExtra("pickupAddress") ?: "Unknown"
        val dropAddress = intent.getStringExtra("destinationAddress") ?: "Unknown"
        val pickup = intent.getStringExtra("pickup") ?: "Unknown"
        val drop = intent.getStringExtra("destination") ?: "Unknown"
        val farmerName = intent.getStringExtra("farmerName") ?: "Unknown"
        val farmerProfileImg = intent.getStringExtra("profileImg")
        val deliveryId = intent.getStringExtra("deliveryId") ?: return
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: ""
        val receiverName = intent.getStringExtra("receiverName") ?: ""
        val receiverNum = intent.getStringExtra("receiverNum") ?: ""

        val profileImageView =
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
        profileImageView.post {
            profileImageView.loadImage(farmerProfileImg)
        }

        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = dropAddress
        findViewById<TextView>(R.id.receiverInfo).text = "Recipient : $receiverName - $receiverNum"
        findViewById<TextView>(R.id.farmerName).text = farmerName

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        fetchDeliveryDetails(deliveryId, pickup, drop, estimatedTime, farmerName)
    }

    @SuppressLint("SetTextI18n")
    private fun fetchDeliveryDetails(
        deliveryId: String,
        pickup: String,
        drop: String,
        estimatedTime: String,
        farmerName: String,

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

            val deliveryRef =
                FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
            deliveryRef.addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

                val requestId = doc.getString("requestId") ?: return@addSnapshotListener

                val requestRef = FirebaseFirestore.getInstance().collection("deliveryRequests")
                    .document(requestId)
                requestRef.addSnapshotListener { req, reqError ->
                    if (reqError != null || req == null || !req.exists()) return@addSnapshotListener

                    val weight = req.get("weight").toString()
                    val productType = req.getString("productType") ?: "Unknown"
                    val purpose = req.getString("purpose") ?: "General"
                    val vehicleId = req.getString("vehicleId") ?: ""
                    val estimatedCost = req.getDouble("estimatedCost") ?: 0.0


                    findViewById<TextView>(R.id.weightAmount).text = "$weight kg"
                    findViewById<TextView>(R.id.productType).text = "$purpose\n$productType"
                    findViewById<TextView>(R.id.totalCost).text =
                        "₱${kotlin.math.ceil(estimatedCost).toInt()}"

                    findViewById<TextView>(R.id.farmerName).text = farmerName
                    findViewById<TextView>(R.id.estimatedTime).text = estimatedTime

                    if (vehicleId.isNotEmpty()) {
                        FirebaseFirestore.getInstance().collection("vehicles")
                            .document(vehicleId)
                            .addSnapshotListener { vehicleDoc, _ ->
                                if (vehicleDoc != null && vehicleDoc.exists()) {
                                    val model = vehicleDoc.getString("model") ?: "Unknown"
                                    val vehicleType =
                                        vehicleDoc.getString("vehicleType") ?: "Unknown"
                                    findViewById<TextView>(R.id.vehicle).text =
                                        "$model\n$vehicleType"
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