package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.ui.farmer.add_delivery.SendPushNotification
import com.ucb.capstone.farmnook.ui.hauler.services.DeliveryLocationService
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import com.ucb.capstone.farmnook.ui.menu.NavigationBar

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        Toast.makeText(this, "🚀 DeliveryDetailsActivity Started", Toast.LENGTH_SHORT).show()

        // Setup bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 200
            isHideable = false
        }

        val pickupAddress = intent.getStringExtra("pickupAddress")
        val destinationAddress = intent.getStringExtra("destinationAddress")
        val pickup = intent.getStringExtra("pickup") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: "N/A"

        // Set delivery info UI
        findViewById<TextView>(R.id.estimatedTime).text = estimatedTime
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = destinationAddress

        // Back button action
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // ✅ New Feature: Start Delivery and Navigate to DeliveryStatusFragment inside NavigationBar
        findViewById<Button>(R.id.startDeliveryBtn).setOnClickListener {
            val haulerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            // Start hauler GPS tracking
            startService(Intent(this, DeliveryLocationService::class.java))

            // Navigate to NavigationBar with intent to show DeliveryStatusFragment
            val deliveryId = intent.getStringExtra("deliveryId") ?: return@setOnClickListener

            val intent = Intent(this, NavigationBar::class.java).apply {
                putExtra("navigateTo", "DeliveryStatus")
                putExtra("deliveryId", deliveryId)
                putExtra("pickup", pickup)
                putExtra("destination", destination)
                putExtra("pickupAddress", pickupAddress)
                putExtra("destinationAddress", destinationAddress)
            }
            startService(Intent(this, DeliveryLocationService::class.java)) // ✅ Keep tracking
            startActivity(intent)
            finish()
        }

        // Done Delivery flow and Firestore history logic
        findViewById<Button>(R.id.doneDeliveryBtn).setOnClickListener {
            val deliveryId = intent.getStringExtra("deliveryId") ?: return@setOnClickListener
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Delivery Completion")
                .setMessage("Are you sure you've completed delivering the products?")
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    val firestore = FirebaseFirestore.getInstance()
                    val deliveryRef = firestore.collection("deliveries").document(deliveryId)
                    deliveryRef.update("status", "completed").addOnSuccessListener {
                        val historyRef = firestore.collection("deliveryHistory").document()
                        val historyId = historyRef.id
                        val completedAt = Timestamp.now()
                        val history = DeliveryHistory(historyId, deliveryId, completedAt, "N/A")
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
                                                val fullName = "${haulerDoc.getString("firstName") ?: ""} ${haulerDoc.getString("lastName") ?: ""}"
                                                val message = "$fullName has completed the delivery."
                                                val title = "Delivery Completed"

                                                SendPushNotification.sendCompletedDeliveryNotification("recipientId", farmerId, deliveryId, title, message, completedAt, this)
                                                SendPushNotification.sendCompletedDeliveryNotification("businessId", businessId, deliveryId, title, message, completedAt, this)

                                                stopService(Intent(this, DeliveryLocationService::class.java))
                                                val nextIntent = Intent(this, HistoryDetailsActivity::class.java).apply {
                                                    putExtra("deliveryId", deliveryId)
                                                    putExtra("pickupAddress", pickupAddress)
                                                    putExtra("destinationAddress", destinationAddress)
                                                }
                                                startActivity(nextIntent)
                                                finish()
                                            }
                                    }
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create().show()
        }

        // Original WebView setup for route preview map (before delivery starts)
        webView = findViewById(R.id.mapView)
        loadMapInWebView(webView, pickup, destination)
    }

    private fun confirmDeliveryCompletion(deliveryId: String, pickupAddress: String?, destinationAddress: String?) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Confirm Delivery Completion")
            .setMessage("Are you sure you've completed delivering the products?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val firestore = FirebaseFirestore.getInstance()
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)
                deliveryRef.update("status", "completed").addOnSuccessListener {
                    val historyRef = firestore.collection("deliveryHistory").document()
                    val historyId = historyRef.id
                    val completedAt = Timestamp.now()
                    val history = DeliveryHistory(historyId, deliveryId, completedAt, "N/A")
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
                                            val fullName = "${haulerDoc.getString("firstName") ?: ""} ${haulerDoc.getString("lastName") ?: ""}"
                                            val message = "$fullName has completed the delivery."
                                            val title = "Delivery Completed"

                                            SendPushNotification.sendCompletedDeliveryNotification("recipientId", farmerId, deliveryId, title, message, completedAt, this)
                                            SendPushNotification.sendCompletedDeliveryNotification("businessId", businessId, deliveryId, title, message, completedAt, this)

                                            stopService(Intent(this, DeliveryLocationService::class.java))
                                            val nextIntent = Intent(this, HistoryDetailsActivity::class.java).apply {
                                                putExtra("deliveryId", deliveryId)
                                                putExtra("pickupAddress", pickupAddress)
                                                putExtra("destinationAddress", destinationAddress)
                                            }
                                            startActivity(nextIntent)
                                            finish()
                                        }
                                }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }
}
