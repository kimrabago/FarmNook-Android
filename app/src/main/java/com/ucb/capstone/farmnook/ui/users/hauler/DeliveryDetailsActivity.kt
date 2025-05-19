package com.ucb.capstone.farmnook.ui.users.hauler

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
import com.ucb.capstone.farmnook.ui.users.hauler.services.DeliveryLocationService
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.utils.SendPushNotification
import com.ucb.capstone.farmnook.utils.loadImage
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("LABEL_NAME_CLASH")
class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var productTypeTextView: TextView
    private lateinit var weightTextView: TextView
    private lateinit var requestDateTextView: TextView
    private lateinit var vehicleTypeTextView: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        val startButton = findViewById<Button>(R.id.startDeliveryBtn)
        val farmerNameTextView = findViewById<TextView>(R.id.farmerName)
        val profileImageView = findViewById<CircleImageView>(R.id.profileImage)
        productTypeTextView = findViewById(R.id.productType)
        weightTextView = findViewById(R.id.weightAmount)
        requestDateTextView = findViewById(R.id.dateTime)
        vehicleTypeTextView = findViewById(R.id.vehicle)


        val pickupAddress = intent.getStringExtra("pickupAddress")
        val destinationAddress = intent.getStringExtra("destinationAddress")
        val pickup = intent.getStringExtra("pickup") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: "N/A"
        val totalCost = intent.getStringExtra("totalCost") ?: "N/A"
        val deliveryId = intent.getStringExtra("deliveryId") ?: return
        val requestId = intent.getStringExtra("requestId") ?: return
        val receiverName = intent.getStringExtra("receiverName") ?: ""
        val receiverNum = intent.getStringExtra("receiverNum") ?: ""
        val deliveryNote = intent.getStringExtra("deliveryNote") ?: ""
        val schedTime = intent.getStringExtra("scheduleTime") ?: ""
        val scheduledTime: Timestamp? = try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = format.parse(schedTime)
            date?.let { Timestamp(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }


        findViewById<TextView>(R.id.receiverInfo).text = "Recipient : $receiverName - $receiverNum"
        val note = findViewById<TextView>(R.id.deliveryNote)

        note.text = "> $deliveryNote"
        note.setBackgroundResource(R.drawable.rounded_gray)
        note.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)

        var isTextVisible = true

        note.setOnClickListener {
            if (isTextVisible) {
                note.text = ""
                note.setBackgroundResource(0) // Remove background
                note.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.notes, 0, 0)
            } else {
                note.text = "> $deliveryNote"
                note.setBackgroundResource(R.drawable.rounded_gray)
                note.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            }
            isTextVisible = !isTextVisible
        }

        findViewById<TextView>(R.id.estimatedTime).text = estimatedTime
        findViewById<TextView>(R.id.totalCost).text = "₱${totalCost}"
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = destinationAddress

        fetchFarmerDetails(requestId, farmerNameTextView, profileImageView, productTypeTextView, weightTextView, requestDateTextView, vehicleTypeTextView)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        scheduledTime?.let {
            val now = java.util.Calendar.getInstance()
            val sched = java.util.Calendar.getInstance().apply { time = it.toDate() }

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val timeString = timeFormat.format(sched.time)

            val isToday = now.get(Calendar.YEAR) == sched.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == sched.get(Calendar.DAY_OF_YEAR)

            val displayText = if (isToday) {
                "Start delivery today at $timeString"
            } else {
                "Start delivery on ${dateFormat.format(sched.time)} at $timeString"
            }

            Log.d("SCHEDULE_TEXT", "Setting button text: $displayText")
            startButton.text = displayText
        } ?: run {
            startButton.text = "Start delivery"
        }

        startButton.setOnClickListener {
            if (scheduledTime == null) {
                Toast.makeText(this, "Invalid schedule time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentTimeMillis = System.currentTimeMillis()
            val scheduledMillis = scheduledTime.toDate().time

            if (currentTimeMillis >= scheduledMillis) {
                // ✅ It's time to start the delivery
                startDelivery(
                    deliveryId, pickup, destination,
                    pickupAddress, destinationAddress,
                    receiverName, receiverNum, deliveryNote
                )
            } else {
                // ❌ Not yet time
                val formatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                val readableTime = formatter.format(scheduledTime.toDate())
                Toast.makeText(this, "⏳ You can start the delivery at $readableTime", Toast.LENGTH_LONG).show()
            }
        }

        webView = findViewById(R.id.mapView)
        loadMapInWebView(webView, pickup, destination)
    }

    private fun startDelivery(
        deliveryId: String,
        pickup: String,
        destination: String,
        pickupAddress: String?,
        destinationAddress: String?,
        receiverName: String?,
        receiverNum: String?,
        deliveryNote: String?,
    ) {
        val haulerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        // ✅ 1. Initialize all flags to proper starting state
        firestore.collection("deliveries").document(deliveryId)
            .update("isStarted", true)

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
            putExtra("receiverName", receiverName)
            putExtra("receiverNum", receiverNum)
            putExtra("deliveryNote", deliveryNote)

        }.also { startActivity(it) }

        finish()
    }
}

@SuppressLint("SetTextI18n")
fun fetchFarmerDetails(
    requestId: String,
    farmerNameTextView: TextView,
    profileImageView: CircleImageView,
    productTypeTextView: TextView,
    weightTextView: TextView,
    requestDateTextView: TextView,
    vehicleTypeTextView: TextView,
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
            val purpose = requestDoc.getString("purpose") ?: return@addSnapshotListener
            val productType = requestDoc.getString("productType") ?: return@addSnapshotListener
            val weight = requestDoc.getString("weight") ?: return@addSnapshotListener
            val vehicleType = requestDoc.getString("purpose") ?: return@addSnapshotListener
            val timestamp = requestDoc.getTimestamp("scheduledTime") ?: return@addSnapshotListener

            val requestDate = timestamp.toDate()
            val formatter = SimpleDateFormat("MMM dd, yyyy: hh:mm a", Locale.US)
            val formattedDate = formatter.format(requestDate)

            requestDateTextView.text = formattedDate
            productTypeTextView.text = "$purpose - $productType"
            weightTextView.text = "$weight kg"
            vehicleTypeTextView.text = vehicleType
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

                    profileImageView.loadImage(profileImageUrl)
                }
        }
}
