package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.data.model.Message
import com.ucb.capstone.farmnook.ui.hauler.services.DeliveryLocationService
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.utils.SendPushNotification
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import de.hdodenhof.circleimageview.CircleImageView

@Suppress("LABEL_NAME_CLASH")
class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        val farmerNameTextView = findViewById<TextView>(R.id.farmerName)
        val profileImageView = findViewById<CircleImageView>(R.id.profileImage)

        //Removed the geocode
        val pickupAddress = intent.getStringExtra("pickupAddress")
        val destinationAddress = intent.getStringExtra("destinationAddress")
        val pickup = intent.getStringExtra("pickup") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: "N/A"
        val totalCost = intent.getStringExtra("totalCost") ?: "N/A"
        val deliveryId = intent.getStringExtra("deliveryId") ?: return
        val requestId = intent.getStringExtra("requestId") ?: return

        findViewById<TextView>(R.id.estimatedTime).text = estimatedTime
        findViewById<TextView>(R.id.totalCost).text = "₱${totalCost}"
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = destinationAddress

        fetchFarmerDetails(requestId, farmerNameTextView, profileImageView)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        findViewById<Button>(R.id.startDeliveryBtn).setOnClickListener {
            startDelivery(deliveryId, pickup, destination, pickupAddress, destinationAddress)
        }
        //Changes
        findViewById<Button>(R.id.doneDeliveryBtn).setOnClickListener {
            confirmDeliveryCompletion(deliveryId, pickup, destination, pickupAddress, destinationAddress)
        }

        webView = findViewById(R.id.mapView)
        loadMapInWebView(webView, pickup, destination)
    }

    private fun startDelivery(
        deliveryId: String,
        pickup: String,
        destination: String,
        pickupAddress: String?,
        destinationAddress: String?
    ) {
        val haulerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        // ✅ 1. Initialize all flags to proper starting state
        firestore.collection("deliveries").document(deliveryId)
            .update(
                mapOf(
                    "isStarted" to true,
                    "arrivedAtPickup" to false,
                    "arrivedAtDestination" to false,
                    "isDone" to false
                )
            )

        // ✅ 2. Notify & setup chat (same logic as before)
        firestore.collection("deliveries").document(deliveryId).get()
            .addOnSuccessListener { deliveryDoc ->
                val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                firestore.collection("deliveryRequests").document(requestId).get()
                    .addOnSuccessListener { reqDoc ->
                        val farmerId = reqDoc.getString("farmerId") ?: return@addOnSuccessListener
                        val chatId = if (haulerId < farmerId) "$haulerId-$farmerId" else "$farmerId-$haulerId"

                        firestore.collection("users").document(haulerId).get()
                            .addOnSuccessListener { haulerDoc ->
                                val fullName = "${haulerDoc.getString("firstName") ?: ""} ${haulerDoc.getString("lastName") ?: ""}".trim()
                                val title = "Delivery Started"
                                val message = "📦 Delivery by $fullName is now **in transit**! Track the delivery in the app."
                                val nowMillis = System.currentTimeMillis()

                                SendPushNotification.sendCompletedDeliveryNotification(
                                    "recipientId", farmerId,
                                    deliveryId, title, message,
                                    Timestamp.now(), this
                                )

                                val businessId = reqDoc.getString("businessId")
                                if (businessId != null) {
                                    SendPushNotification.sendCompletedDeliveryNotification(
                                        "businessId", businessId,
                                        deliveryId, title, message,
                                        Timestamp.now(), this
                                    )
                                }

                                val chatRef = firestore.collection("chats").document(chatId)
                                chatRef.set(
                                    mapOf(
                                        "userIds" to listOf(haulerId, farmerId),
                                        "lastMessage" to message,
                                        "timestamp" to nowMillis
                                    ),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )

                                val autoMsg = Message(
                                    senderId = haulerId,
                                    receiverId = farmerId,
                                    content = message,
                                    timestamp = nowMillis,
                                    senderName = fullName
                                )
                                chatRef.collection("messages").add(autoMsg)
                            }
                    }
            }

        // ✅ 3. Start tracking & open DeliveryStatus
        startService(Intent(this, DeliveryLocationService::class.java))
        Intent(this, NavigationBar::class.java).apply {
            putExtra("navigateTo", "DeliveryStatus")
            putExtra("deliveryId", deliveryId)
            putExtra("pickup", pickup)
            putExtra("destination", destination)
            putExtra("pickupAddress", pickupAddress)
            putExtra("destinationAddress", destinationAddress)
        }.also { startActivity(it) }

        finish()
    }

    private fun confirmDeliveryCompletion(
        deliveryId: String,
        pickup: String,
        destination: String,
        pickupAddress: String?,
        destinationAddress: String?
    ) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delivery Completion")
            .setMessage("Are you sure you've completed delivering the products?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                val firestore = FirebaseFirestore.getInstance()
                val deliveryRef = firestore.collection("deliveries").document(deliveryId)

                deliveryRef.update("status", "completed")
                    .addOnSuccessListener {
                        val historyRef = firestore.collection("deliveryHistory").document()
                        val completedAt = Timestamp.now()
                        val history = DeliveryHistory(historyRef.id, deliveryId, completedAt, "N/A")

                        historyRef.set(history).addOnSuccessListener {
                            deliveryRef.get().addOnSuccessListener { deliveryDoc ->
                                val requestId = deliveryDoc.getString("requestId")
                                    ?: return@addOnSuccessListener
                                val haulerId = deliveryDoc.getString("haulerAssignedId")
                                    ?: return@addOnSuccessListener

                                firestore.collection("deliveryRequests").document(requestId).get()
                                    .addOnSuccessListener { reqDoc ->
                                        val farmerId = reqDoc.getString("farmerId")
                                            ?: return@addOnSuccessListener
                                        val businessId = reqDoc.getString("businessId")
                                            ?: return@addOnSuccessListener
                                        val estimatedTime =
                                            reqDoc.getString("estimatedTime") ?: "N/A"


                                        val chatId = if (haulerId < farmerId) "$haulerId-$farmerId"
                                        else "$farmerId-$haulerId"

                                        firestore.collection("users").document(farmerId).get()
                                            .addOnSuccessListener { farmerDoc ->
                                                val farmerName =
                                                    "${farmerDoc.getString("firstName") ?: ""} ${
                                                        farmerDoc.getString("lastName") ?: ""
                                                    }".trim()
                                                val profileImage =
                                                    farmerDoc.getString("profileImageUrl") ?: "N/A"

                                                //Hauler
                                                firestore.collection("users").document(haulerId)
                                                    .get()
                                                    .addOnSuccessListener { haulerDoc ->
                                                        val fullName =
                                                            "${haulerDoc.getString("firstName") ?: ""} " +
                                                                    (haulerDoc.getString("lastName") ?: "").trim()
                                                        val title = "Delivery Completed"
                                                        val message =
                                                            "$fullName has completed the delivery."
                                                        val nowMillis = completedAt.toDate().time


                                                        SendPushNotification.sendCompletedDeliveryNotification(
                                                            "recipientId", farmerId,
                                                            deliveryId, title, message,
                                                            completedAt, this
                                                        )
                                                        SendPushNotification.sendCompletedDeliveryNotification(
                                                            "businessId", businessId,
                                                            deliveryId, title, message,
                                                            completedAt, this
                                                        )

                                                        val chatRef =
                                                            firestore.collection("chats")
                                                                .document(chatId)
                                                        chatRef.set(
                                                            mapOf(
                                                                "userIds" to listOf(
                                                                    haulerId,
                                                                    farmerId
                                                                ),
                                                                "lastMessage" to message,
                                                                "timestamp" to nowMillis
                                                            ),
                                                            com.google.firebase.firestore.SetOptions.merge()
                                                        )

                                                        val autoMsg = Message(
                                                            senderId = haulerId,
                                                            receiverId = farmerId,
                                                            content = message,
                                                            timestamp = nowMillis,
                                                            senderName = fullName
                                                        )
                                                        chatRef.collection("messages").add(autoMsg)

                                                        stopService(
                                                            Intent(
                                                                this,
                                                                DeliveryLocationService::class.java
                                                            )
                                                        )
                                                        Intent(
                                                            this,
                                                            HistoryDetailsActivity::class.java
                                                        ).apply {
                                                            putExtra("deliveryId", deliveryId)
                                                            //Changes
                                                            putExtra("pickup", pickup)
                                                            putExtra("destination", destination)
                                                            putExtra("pickupAddress", pickupAddress)
                                                            putExtra("destinationAddress", destinationAddress)
                                                            putExtra("estimatedTime", estimatedTime)
                                                            putExtra("farmerName", farmerName)
                                                            putExtra("profileImg", profileImage)
                                                        }.also { startActivity(it) }
                                                        finish()
                                                    }
                                            }
                                    }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to complete delivery: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("No") { dlg, _ -> dlg.dismiss() }
            .create().show()
    }
}

fun fetchFarmerDetails(
    requestId: String,
    farmerNameTextView: TextView,
    profileImageView: CircleImageView
) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("deliveryRequests").document(requestId)
        .addSnapshotListener { requestDoc, error ->
            if (error != null || requestDoc == null || !requestDoc.exists()) {
                Toast.makeText(
                    farmerNameTextView.context,
                    "Failed to listen to request",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val farmerId = requestDoc.getString("farmerId") ?: return@addSnapshotListener

            // Now listen to the farmer's user document
            firestore.collection("users").document(farmerId)
                .addSnapshotListener { farmerDoc, err ->
                    if (err != null || farmerDoc == null || !farmerDoc.exists()) {
                        Toast.makeText(
                            farmerNameTextView.context,
                            "Failed to listen to farmer",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addSnapshotListener
                    }
                    val profileImageUrl = farmerDoc.getString("profileImageUrl")
                    val fullName =
                        "${farmerDoc.getString("firstName") ?: ""} ${farmerDoc.getString("lastName") ?: ""}".trim()
                    farmerNameTextView.text = fullName

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(profileImageView.context)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.profile_circle)
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.profile_circle)
                    }
                }
        }
}

