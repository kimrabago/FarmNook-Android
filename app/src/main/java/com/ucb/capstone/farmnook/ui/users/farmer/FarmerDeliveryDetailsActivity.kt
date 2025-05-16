package com.ucb.capstone.farmnook.ui.users.farmer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.*
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import com.ucb.capstone.farmnook.utils.loadImage
import com.ucb.capstone.farmnook.utils.loadMapInWebView
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.ceil

class FarmerDeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loadingLayout: View
    private lateinit var confirmationLayout: View
    private lateinit var noActiveDeliveryLayout: View
    private lateinit var webView: WebView
    private lateinit var haulerProfileImage: CircleImageView
    private lateinit var haulerNameTxtView: TextView
    private lateinit var haulerLicenseNoTxtView: TextView
    private lateinit var haulerPhoneNumTxtView: TextView

    private lateinit var deliveryRequestRef: DocumentReference
    private var deliveryId: String? = null
    private var listenerRegistration: ListenerRegistration? = null
    private var argPickupName: String? = null
    private var argPurpose: String? = null
    private var argDestinationName: String? = null
    private var argProductType: String? = null
    private var argWeight: String? = null
    private var argTotalCost: Double? = null
    private var argEstimatedTime: String? = null
    private var argBusinessName: String? = null
    private var argLocationName: String? = null
    private var argProfileImage: String? = null
    private var argVehicleType: String? = null
    private var argVehicleModel: String? = null
    private var argPlateNumber: String? = null
    private lateinit var pickup: String
    private lateinit var destination: String

    private lateinit var vehicleWtBusiness: VehicleWithBusiness
    private lateinit var deliveryReq: DeliveryRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farmer_delivery_details)

        // Initialize views
        webView = findViewById(R.id.mapView)
        haulerProfileImage = findViewById(R.id.profileImage)
        haulerNameTxtView = findViewById(R.id.haulerName)
        haulerLicenseNoTxtView = findViewById(R.id.licenseNo)
        haulerPhoneNumTxtView  = findViewById(R.id.phoneNumber)
        loadingLayout = findViewById(R.id.loadingLayout)
        confirmationLayout = findViewById(R.id.confirmationLayout)
        noActiveDeliveryLayout = findViewById(R.id.noActiveDeliveryLayout)

        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Setup WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = WebViewClient()

        // Setup bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Get intent extras
        val requestId = intent.getStringExtra("requestId")
        argPickupName = intent.getStringExtra("pickupName")
        argDestinationName = intent.getStringExtra("destinationName")
        argPurpose = intent.getStringExtra("purpose")
        argProductType = intent.getStringExtra("productType")
        argWeight = intent.getStringExtra("weight")
        argBusinessName = intent.getStringExtra("businessId")
        argTotalCost = intent.getDoubleExtra("estimatedCost", -1.0)
        argEstimatedTime = intent.getStringExtra("estimatedTime")
        argBusinessName = intent.getStringExtra("businessName")
        argLocationName = intent.getStringExtra("locationName")
        argProfileImage = intent.getStringExtra("profileImageUrl")
        argVehicleType = intent.getStringExtra("vehicleType")
        argVehicleModel = intent.getStringExtra("vehicleModel")
        argPlateNumber = intent.getStringExtra("plateNumber")

        // Setup message button
        findViewById<ImageButton>(R.id.messageIcon).setOnClickListener {
            val deliveryId = this.deliveryId ?: return@setOnClickListener

            FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId).get()
                .addOnSuccessListener { deliveryDoc ->
                    val farmerId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        ?: return@addOnSuccessListener
                    val haulerId = deliveryDoc.getString("haulerAssignedId")
                        ?: return@addOnSuccessListener

                    val chatId = if (farmerId < haulerId) "$farmerId-$haulerId"
                    else "$haulerId-$farmerId"

                    FirebaseFirestore.getInstance().collection("users").document(haulerId).get()
                        .addOnSuccessListener { haulerDoc ->
                            val firstName = haulerDoc.getString("firstName") ?: ""
                            val lastName = haulerDoc.getString("lastName") ?: ""
                            val haulerName = "$firstName $lastName".trim()

                            Intent(this, MessageActivity::class.java).apply {
                                putExtra("chatId", chatId)
                                putExtra("recipientId", haulerId)
                                putExtra("receiverName", haulerName)
                            }.also { startActivity(it) }
                        }
                }
        }

        // Setup cancel button
        findViewById<Button>(R.id.cancelButton).setOnClickListener { cancelDeliveryRequest() }

        // Setup summary button
        val summaryButton1 = findViewById<Button?>(R.id.viewSummaryButton)
        val summaryButton2 = findViewById<Button?>(R.id.viewSummaryButton1)

        val clickListener = View.OnClickListener {
            if (::deliveryReq.isInitialized) {
                showDeliverySummaryDialog()
            } else {
                Toast.makeText(this, "Delivery details are still loading.", Toast.LENGTH_SHORT).show()
            }
        }

// Attach to both if present
        summaryButton1?.setOnClickListener(clickListener)
        summaryButton2?.setOnClickListener(clickListener)

        if (requestId.isNullOrEmpty()) {
            showNoActiveDelivery()
            return
        }

        deliveryRequestRef = FirebaseFirestore.getInstance().collection("deliveryRequests").document(requestId)

        deliveryRequestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                showNoActiveDelivery()
                return@addOnSuccessListener
            }

            pickup = doc.getString("pickupLocation") ?: ""
            destination = doc.getString("destinationLocation") ?: ""

            if (pickup.isNotEmpty() && destination.isNotEmpty()) {
                val pickupLocName = argPickupName ?: pickup
                val destLocName = argDestinationName ?: destination

                deliveryReq = DeliveryRequest(
                    pickupLocation = pickup,
                    pickupName = pickupLocName,
                    destinationLocation = destination,
                    destinationName = destLocName,
                    productType = argProductType,
                    weight = argWeight
                )

                fetchDeliveryIdAndLoadStatus(doc.id, pickup, destination)
            } else {
                Toast.makeText(this, "Missing pickup/destination", Toast.LENGTH_SHORT).show()
                showNoActiveDelivery()
            }
        }

        listenForDeliveryConfirmation()
    }

    private fun showDeliverySummaryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delivery_summary, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = builder.create()

        val businessName = dialogView.findViewById<TextView>(R.id.businessName)
        val businessLocation = dialogView.findViewById<TextView>(R.id.businessLocation)
        val profileImage = dialogView.findViewById<CircleImageView>(R.id.profileImage)

        businessName.text = argBusinessName
        businessLocation.text = argLocationName
        profileImage.loadImage(argProfileImage)

        // Reusable rows
        fun setRowText(viewId: Int, label: String, value: String) {
            val row = dialogView.findViewById<View>(viewId)
            row.findViewById<TextView>(R.id.label).text = label
            row.findViewById<TextView>(R.id.value).text = value
        }

        // Populate rows
        setRowText(R.id.plateRow, "Plate Number", argPlateNumber ?: "N/A")
        setRowText(R.id.pickupRow, "Pickup", argPickupName ?: "N/A")
        setRowText(R.id.destinationRow, "Destination", argDestinationName ?: "N/A")
        setRowText(
            R.id.vehicleRow,
            "Vehicle",
            "${(argVehicleType ?: "N/A")} - ${(argVehicleModel ?: "N/A")}"
        )
        setRowText(
            R.id.purposeRow,
            "Product",
            "${(argPurpose ?: "N/A").replaceFirstChar { it.uppercase() }} - ${argProductType ?: "N/A"} (${argWeight ?: "N/A"} kg)"
        )
        val roundedCost = argTotalCost?.let { ceil(it).toInt() } ?: "N/A"
        setRowText(R.id.productRow, "Cost", "₱$roundedCost")
        setRowText(R.id.weightRow, "Time", argEstimatedTime ?: "N/A")

        dialogView.findViewById<Button>(R.id.cancelButton).apply {
            text = "CLOSE"
            setOnClickListener {
                dialog.dismiss()
            }
        }

        dialogView.findViewById<Button>(R.id.hireButton).apply {
            visibility = View.GONE
        }

        dialog.show()
    }

    private fun fetchDeliveryIdAndLoadStatus(
        requestId: String,
        pickup: String,
        destination: String,
    ) {
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
                val haulerId =
                    deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                val encodedPickup = pickup.replace(" ", "")
                val encodedDrop = destination.replace(" ", "")
                val mapUrl =
                    "https://farmnook-web.vercel.app/live-tracking?pickup=$encodedPickup&drop=$encodedDrop&haulerId=$haulerId"

                Log.d("MAP_DEBUG", "🌍 Loading map URL: $mapUrl")
                webView.loadUrl(mapUrl)

                listenToDeliveryStatus(deliveryId!!)
                fetchHaulerDetails(haulerId)
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Failed to get delivery details",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @SuppressLint("SetTextI18n")
    private fun listenToDeliveryStatus(deliveryId: String) {
        val deliveryRef = FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
        listenerRegistration = deliveryRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val isDone = snapshot.getBoolean("isDone") ?: false
            val arrivedAtDestination = snapshot.getBoolean("arrivedAtDestination") ?: false
            val arrivedAtPickup = snapshot.getBoolean("arrivedAtPickup") ?: false
            val isStarted = snapshot.getBoolean("isStarted") ?: false
            val isOnDelivery = snapshot.getBoolean("isOnDelivery") ?: false

            val statusTextView = findViewById<TextView>(R.id.status)

            when {
                isDone -> {
                    showCompletedMessage()
                    statusTextView?.text = "✅ Delivery Completed"
                    statusTextView?.setTextColor(resources.getColor(R.color.purple, theme))
                }
                arrivedAtDestination -> {
                    showConfirmationLayout()
                    statusTextView?.text = "🚩 Arrived at Destination"
                    statusTextView?.setTextColor(resources.getColor(R.color.dark_green, theme))
                }
                isOnDelivery -> {
                    showConfirmationLayout()
                    statusTextView?.text = "🚚 On Delivery"
                    statusTextView?.setTextColor(resources.getColor(R.color.light_green, theme))
                }
                arrivedAtPickup -> {
                    showConfirmationLayout()
                    statusTextView?.text = "📍 Arrived at Pickup"
                    statusTextView?.setTextColor(resources.getColor(R.color.yellow, theme))
                }
                isStarted -> {
                    showConfirmationLayout()
                    statusTextView?.text = "🚚 Going to Pickup"
                    statusTextView?.setTextColor(resources.getColor(R.color.orange, theme))
                }
                else -> {
                    showConfirmationLayout()
                    statusTextView?.text = "⏳ Waiting for Hauler"
                    statusTextView?.setTextColor(resources.getColor(R.color.gray, theme))
                }
            }
        }
    }

    private fun listenForDeliveryConfirmation() {
        deliveryRequestRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(
                    this,
                    "Failed to listen for updates.",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun showLoadingLayout() {
        loadingLayout.visibility = View.VISIBLE
        confirmationLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE

        if (::deliveryReq.isInitialized) {
            loadMapInWebView(webView, pickup, destination)
        }
    }

    private fun showConfirmationLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
        noActiveDeliveryLayout.visibility = View.GONE
    }

    private fun showNoActiveDelivery() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE

        webView.setBackgroundColor(Color.parseColor("#E0E0E0"))
        webView.loadData(
            "<html><body style='background-color:#E0E0E0;'><h3 style='color:#888; text-align:center; margin-top:50%;'>No Active Delivery</h3></body></html>",
            "text/html", "utf-8"
        )
    }

    private fun showCompletedMessage() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
        noActiveDeliveryLayout.visibility = View.GONE
        
        // Update UI elements for completed state
        findViewById<TextView>(R.id.status)?.apply {
            text = "✅ Delivery Completed"
            setTextColor(resources.getColor(R.color.dark_green, theme))
        }
        
        // Disable cancel button for completed deliveries
        findViewById<Button>(R.id.cancelButton)?.apply {
            visibility = View.GONE
        }
    }

    private fun cancelDeliveryRequest() {
        deliveryRequestRef.update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Delivery request cancelled successfully.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to cancel delivery request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchHaulerDetails(haulerId: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(haulerId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val haulerName =
                    document.getString("firstName") + " " + document.getString("lastName")
                val haulerProfileImg = document.getString("profileImageUrl")
                val haulerPhoneNum = document.getString("phoneNum")
                val haulerLicenseNum = document.getString("licenseNo")

                haulerProfileImage.loadImage(haulerProfileImg)

                haulerNameTxtView.text = haulerName
                haulerLicenseNoTxtView.text = haulerLicenseNum
                haulerPhoneNumTxtView.text = haulerPhoneNum
                showConfirmationLayout()
            } else {
                Log.w("DeliveryDetailsActivity", "Hauler not found with ID: $haulerId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryDetailsActivity", "Error fetching hauler details: ${e.message}")
        }
    }

    private fun fetchDeliveryDetails(requestId: String) {
        val deliveriesRef = FirebaseFirestore.getInstance()
            .collection("deliveries")
            .whereEqualTo("requestId", requestId)

        deliveriesRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val deliveryDoc = querySnapshot.documents[0]
                val haulerId =
                    deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener
                val deliveryId = deliveryDoc.id

                val deliveryIdTextView = findViewById<TextView>(R.id.deliveryId)
                deliveryIdTextView?.text = "Delivery ID: $deliveryId"

                fetchHaulerDetails(haulerId)
            } else {
                Log.w("DeliveryDetailsActivity", "No delivery found for requestId: $requestId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryDetailsActivity", "Error fetching delivery details: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
} 