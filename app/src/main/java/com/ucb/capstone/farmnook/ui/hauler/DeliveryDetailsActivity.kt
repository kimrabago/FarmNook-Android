package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.ui.farmer.add_delivery.SendPushNotification

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetJavaScriptEnabled")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DeliveryDetailsActivity", "onCreate started")

        super.onCreate(savedInstanceState)
        Toast.makeText(this, "🚀 DeliveryDetailsActivity Started", Toast.LENGTH_SHORT).show()

        setContentView(R.layout.activity_delivery_details)

        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false

        val pickupAddress = intent.getStringExtra("pickupAddress")
        val destinationAddress = intent.getStringExtra("destinationAddress")
        val pickup = intent.getStringExtra("pickup") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: "N/A"

        // Set delivery info
        findViewById<TextView>(R.id.estimatedTime).text = estimatedTime
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = destinationAddress

        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.doneDeliveryBtn).setOnClickListener {
            val deliveryId = intent.getStringExtra("deliveryId") ?: return@setOnClickListener

            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Delivery Completion")
            builder.setMessage("Are you sure you’ve completed delivering the products?")

            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss() // ✅ Only dismiss the dialog immediately

                val firestore = FirebaseFirestore.getInstance()
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)

                // 1. Update status
                deliveryRef.update("status", "completed").addOnSuccessListener {
                    // 2. Add to deliveryHistory
                    val historyRef = firestore.collection("deliveryHistory").document()
                    val historyId = historyRef.id
                    val completedAt = Timestamp.now()

                    val history = DeliveryHistory(
                        historyId = historyId,
                        deliveryId = deliveryId,
                        deliveryArrivalTime = completedAt,
                        distanceTraveled = "N/A"
                    )

                    historyRef.set(history).addOnSuccessListener {
                        deliveryRef.get().addOnSuccessListener { deliveryDoc ->
                            val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                            val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                            firestore.collection("deliveryRequests").document(requestId).get()
                                .addOnSuccessListener { reqDoc ->
                                    val farmerId = reqDoc.getString("farmerId") ?: return@addOnSuccessListener
                                    val businessId = reqDoc.getString("businessId") ?: return@addOnSuccessListener

                                    firestore.collection("users").document(haulerId).get()
                                        .addOnSuccessListener { haulerDoc ->
                                            val firstName = haulerDoc.getString("firstName") ?: "Hauler"
                                            val lastName = haulerDoc.getString("lastName") ?: ""
                                            val fullName = "$firstName $lastName"
                                            val message = "$fullName has completed the delivery."
                                            val title = "Delivery Completed"

                                            SendPushNotification.sendCompletedDeliveryNotification("farmerId", farmerId, deliveryId, title, message, completedAt, this)
                                            SendPushNotification.sendCompletedDeliveryNotification("businessId", businessId, deliveryId, title, message, completedAt, this)


                                            // ✅ Navigate
                                            val originalIntent = intent
                                            val nextIntent = Intent(this, HistoryDetailsActivity::class.java).apply {
                                                putExtra("deliveryId", deliveryId)
                                                putExtra("pickupAddress", originalIntent.getStringExtra("pickupAddress"))
                                                putExtra("destinationAddress", originalIntent.getStringExtra("destinationAddress"))
                                            }
                                            startActivity(nextIntent)
                                            finish()
                                        }
                                }
                        }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }
        // Initialize WebView
        webView = findViewById(R.id.mapView)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()


        // Load your deployed Map (React Leaflet)
        val encodedPickup = pickup.replace(" ", "")
        val encodedDrop = destination.replace(" ", "")
        Log.d("DeliveryDetailsActivity", "Encoded Pickup: $encodedPickup")
        Log.d("DeliveryDetailsActivity", "Encoded Drop: $encodedDrop")
        val mapUrl = "https://farmnook-web.vercel.app/map-viewer?pickup=$encodedPickup&drop=$encodedDrop"
        webView.loadUrl(mapUrl)
    }
}
