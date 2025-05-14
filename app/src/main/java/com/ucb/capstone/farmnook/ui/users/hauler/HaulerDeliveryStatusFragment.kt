package com.ucb.capstone.farmnook.ui.users.hauler

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.*
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.Message
import com.ucb.capstone.farmnook.ui.users.hauler.services.DeliveryLocationService
import com.ucb.capstone.farmnook.ui.users.HistoryDetailsActivity
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import com.ucb.capstone.farmnook.utils.SendPushNotification
import com.ucb.capstone.farmnook.utils.loadImage
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.*
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

class HaulerDeliveryStatusFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var webViewLoaded = false
    private var lastLocation: Location? = null
    private var deliveryId: String? = null
    private var pickupCoords: String? = null
    private var dropCoords: String? = null
    private var pickupAddress: String? = null
    private var destinationAddress: String? = null
    private var receiverName: String? = null
    private var receiverNum: String? = null
    private var deliveryNote: String? = null
    private var farmerId: String? = null
    private var farmerName: String? = null
    private var profileImage: String? = null
    private var vehicleId: String? = null
    private var currentStatus = "Going to Pickup"
    private var haulerId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var deliveryReq: DeliveryRequest
    private lateinit var deliveryRequestRef: DocumentReference

    private var isTextVisible = true

    private val firestore = FirebaseFirestore.getInstance()
    private val mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hauler_delivery_status, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val summaryButton = view.findViewById<ImageView>(R.id.deliverySummaryBtn)
        summaryButton.setOnClickListener {
            if (::deliveryReq.isInitialized) {
                showDeliverySummaryDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Delivery details are still loading.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val messageIcon = view.findViewById<ImageButton>(R.id.messageIcon)
        messageIcon.setOnClickListener {
            val deliveryId = arguments?.getString("deliveryId") ?: return@setOnClickListener

            FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId).get()
                .addOnSuccessListener { deliveryDoc ->
                    val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                    val haulerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener

                    FirebaseFirestore.getInstance().collection("deliveryRequests").document(requestId).get()
                        .addOnSuccessListener { reqDoc ->
                            val farmerId = reqDoc.getString("farmerId") ?: return@addOnSuccessListener

                            // Generate chat ID (consistent with your existing format)
                            val chatId = if (haulerId < farmerId) "$haulerId-$farmerId" else "$farmerId-$haulerId"

                            // Fetch farmer's details to get their name
                            FirebaseFirestore.getInstance().collection("users").document(farmerId).get()
                                .addOnSuccessListener { farmerDoc ->
                                    val firstName = farmerDoc.getString("firstName") ?: ""
                                    val lastName = farmerDoc.getString("lastName") ?: ""
                                    val farmerName = "$firstName $lastName".trim()

                                    // Start MessageActivity with all details
                                    Intent(requireContext(), MessageActivity::class.java).apply {
                                        putExtra("chatId", chatId)
                                        putExtra("recipientId", farmerId)
                                        putExtra("receiverName", farmerName)
                                        // Add any other needed extras
                                    }.also { startActivity(it) }
                                }
                                .addOnFailureListener {
                                    // Fallback if farmer details can't be fetched
                                    Intent(requireContext(), MessageActivity::class.java).apply {
                                        putExtra("chatId", chatId)
                                        putExtra("recipientId", farmerId)
                                        putExtra("receiverName", "Farmer")
                                    }.also { startActivity(it) }
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(),
                                "Failed to load request details",
                                Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(),
                        "Failed to load delivery details",
                        Toast.LENGTH_SHORT).show()
                }
        }

        webView = view.findViewById(R.id.mapView)
        setupWebView()

        val note = view.findViewById<TextView>(R.id.deliveryNote)
        val fromAddress = view.findViewById<TextView>(R.id.from_address)
        val toAddress = view.findViewById<TextView>(R.id.to_address)
        val receiverInfo = view.findViewById<TextView>(R.id.receiverInfo)
        val totalKilometer = view.findViewById<TextView>(R.id.totalKilometer)
        val durationTime = view.findViewById<TextView>(R.id.durationTime)
        val status = view.findViewById<TextView>(R.id.status)
        val doneDeliveryBtn = view.findViewById<Button>(R.id.doneDeliveryBtn)



        doneDeliveryBtn.setOnClickListener {
            deliveryId?.let { id ->
                showCompletionConfirmation(id)
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        deliveryId = arguments?.getString("deliveryId")
        if (deliveryId != null) {
            fetchDeliveryData(deliveryId!!, fromAddress, toAddress, totalKilometer, durationTime, status, receiverInfo, note)
        }



        // Check initial delivery status
        deliveryId?.let { id ->
            Log.d("DeliveryStatus", "Checking initial delivery status for ID: $id")
            firestore.collection("deliveries").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    val atDestination = doc.getBoolean("arrivedAtDestination") ?: false
                    Log.d("DeliveryStatus", "Initial state - arrivedAtDestination: $atDestination")
                    if (atDestination) {
                        Log.d("DeliveryStatus", "Setting initial button visibility to VISIBLE")
                        doneDeliveryBtn.visibility = View.VISIBLE
                        Log.d("DeliveryStatus", "Button visibility after initial update: ${doneDeliveryBtn.visibility}")
                    }
                }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun showDeliverySummaryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delivery_summary, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
        val dialog = builder.create()

        val businessName = dialogView.findViewById<TextView>(R.id.businessName)
        val businessLocation = dialogView.findViewById<TextView>(R.id.businessLocation)
        val profileImage = dialogView.findViewById<CircleImageView>(R.id.profileImage)

        businessName.text = deliveryReq.businessName
        businessLocation.visibility = View.GONE
        profileImage.loadImage(deliveryReq.profileImageUrl)

        fun setRowText(viewId: Int, label: String, value: String) {
            val row = dialogView.findViewById<View>(viewId)
            row.findViewById<TextView>(R.id.label).text = label
            row.findViewById<TextView>(R.id.value).text = value
        }

//        // Populate rows
        setRowText(R.id.plateRow, "Plate Number", deliveryReq.plateNumber ?: "N/A")
        setRowText(R.id.pickupRow, "Pickup", deliveryReq.pickupName ?: "N/A")
        setRowText(R.id.destinationRow, "Destination", deliveryReq.destinationName ?: "N/A")
        setRowText(
            R.id.vehicleRow,
            "Vehicle",
            "${(deliveryReq.vehicleType ?: "N/A")} - ${(deliveryReq.vehicleModel ?: "N/A")}"
        )
        setRowText(
            R.id.purposeRow,
            "Product",
            "${(deliveryReq.purpose ?: "N/A").replaceFirstChar { it.uppercase() }} - ${deliveryReq.productType ?: "N/A"} (${deliveryReq.weight ?: "N/A"} kg)"
        )
        val roundedCost = deliveryReq.estimatedCost?.let { ceil(it).toInt() } ?: "N/A"
        setRowText(R.id.productRow, "Cost", "₱$roundedCost")

        val scheduledTimeStr = deliveryReq.scheduledTime?.toDate()?.let { date ->
            val formatter = SimpleDateFormat("EEE MMM dd, yyyy, hh:mm a", Locale.getDefault())
            formatter.format(date)
        } ?: "N/A"
        setRowText(R.id.weightRow, "Scheduled Time", scheduledTimeStr)

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

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebViewConsole", "JS: ${consoleMessage?.message()}")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webViewLoaded = true
                if (isAdded && context != null) {
                    checkPermissionsAndStartLocation()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchDeliveryData(
        deliveryId: String,
        fromAddress: TextView,
        toAddress: TextView,
        totalKilometer: TextView,
        durationTime: TextView,
        status: TextView,
        receiverInfo: TextView,
        note: TextView
    ) {
        Log.d("DeliveryStatus", "Fetching delivery data for ID: $deliveryId")
        firestore.collection("deliveries").document(deliveryId)
            .addSnapshotListener { deliveryDoc, _ ->
                if (deliveryDoc == null || !deliveryDoc.exists()) {
                    Log.d("DeliveryStatus", "Delivery document doesn't exist")
                    return@addSnapshotListener
                }

                val requestId = deliveryDoc.getString("requestId")
                if (requestId == null) {
                    Log.e("DeliveryStatus", "⚠️ requestId is null from delivery document!")
                    return@addSnapshotListener
                }

                haulerId = deliveryDoc.getString("haulerAssignedId") ?: haulerId

                val isStarted = deliveryDoc.getBoolean("isStarted") ?: false
                val isArrivedAtPickup = deliveryDoc.getBoolean("arrivedAtPickup") ?: false
                val isArrivedAtDestination = deliveryDoc.getBoolean("arrivedAtDestination") ?: false
                val isDone = deliveryDoc.getBoolean("isDone") ?: false

                Log.d("DeliveryStatus", "Delivery state - isStarted: $isStarted, isArrivedAtPickup: $isArrivedAtPickup, isArrivedAtDestination: $isArrivedAtDestination, isDone: $isDone")

                currentStatus = when {
                    !isStarted && !isArrivedAtPickup && !isArrivedAtDestination && !isDone -> "Pending"
                    isStarted && !isArrivedAtPickup && !isArrivedAtDestination && !isDone -> "Going to Pickup"
                    isStarted && isArrivedAtPickup && !isArrivedAtDestination && !isDone -> "On the Way"
                    isStarted && isArrivedAtPickup && isArrivedAtDestination && !isDone -> "Arrived at Destination"
                    isStarted && isArrivedAtPickup && isArrivedAtDestination && isDone -> "Completed"
                    else -> "Pending"
                }

                status.text = currentStatus
                injectStatusToWebView()

                // Update button visibility based on delivery status
                view?.findViewById<Button>(R.id.doneDeliveryBtn)?.let { button ->
                    if (isArrivedAtDestination && !isDone) {
                        Log.d("DeliveryStatus", "Setting button visibility to VISIBLE in fetchDeliveryData")
                        button.visibility = View.VISIBLE
                    } else {
                        Log.d("DeliveryStatus", "Setting button visibility to GONE in fetchDeliveryData")
                        button.visibility = View.GONE
                    }
                }

                firestore.collection("deliveryRequests").document(requestId)
                    .get()
                    .addOnSuccessListener { req ->
                        pickupCoords = req.getString("pickupLocation")
                        dropCoords = req.getString("destinationLocation")
                        pickupAddress =req.getString("pickupName")
                        destinationAddress = req.getString("destinationName")
                        receiverName = req.getString("receiverName") ?: "Unknown"
                        receiverNum = req.getString("receiverNumber") ?: "Unknown"
                        deliveryNote = req.getString("deliveryNote") ?: "Unknown"
                        farmerId = req.getString("farmerId") ?: "Unknown"
                        vehicleId = req.getString("vehicleId") ?: "Unknown"

                        firestore.collection("users").document(farmerId!!)
                            .get()
                            .addOnSuccessListener { businessDoc ->
                                val firstName = businessDoc.getString("firstName") ?: ""
                                val lastName = businessDoc.getString("lastName") ?: ""
                                farmerName = "$firstName $lastName".trim()
                                profileImage = businessDoc.getString("profileImageUrl") ?: ""

                                firestore.collection("vehicles").document(vehicleId!!)
                                    .get()
                                    .addOnSuccessListener { vehicleDoc ->
                                        deliveryReq = DeliveryRequest(
                                            pickupLocation = req.getString("pickupLocation") ?: "",
                                            destinationLocation = req.getString("destinationLocation") ?: "",
                                            pickupName = pickupAddress,
                                            destinationName = destinationAddress,
                                            productType = req.getString("productType") ?: "",
                                            weight = req.getString("weight") ?: "",
                                            purpose = req.getString("purpose") ?: "",
                                            businessId = req.getString("businessId") ?: "",
                                            farmerId = req.getString("farmerId") ?: "",
                                            isAccepted = req.getBoolean("isAccepted") ?: false,
                                            receiverName = req.getString("receiverName"),
                                            receiverNumber = req.getString("receiverNumber"),
                                            deliveryNote = deliveryNote,
                                            estimatedTime = req.getString("estimatedTime"),
                                            estimatedCost = req.getDouble("estimatedCost") ?: 0.0,
                                            scheduledTime = req.getTimestamp("scheduledTime"),
                                            businessName = farmerName,
                                            profileImageUrl = profileImage,
                                            vehicleType = vehicleDoc.getString("vehicleType") ?: "",
                                            vehicleModel = vehicleDoc.getString("model") ?: "",
                                            plateNumber = vehicleDoc.getString("plateNumber") ?: ""
                                        )

                                        // ✅ Now initialize the UI after deliveryReq is ready
                                        note.text = "> ${deliveryNote}"
                                        note.setBackgroundResource(R.drawable.rounded_gray)
                                        note.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                                        isTextVisible = true

                                        note.setOnClickListener {
                                            if (isTextVisible) {
                                                note.text = ""
                                                note.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.notes, 0, 0)
                                                note.setBackgroundColor(Color.TRANSPARENT)
                                            } else {
                                                note.text = "> ${deliveryNote}"
                                                note.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                                                note.setBackgroundResource(R.drawable.rounded_gray)
                                            }
                                            isTextVisible = !isTextVisible
                                        }

                                        fromAddress.text = pickupAddress
                                        toAddress.text = destinationAddress
                                        receiverInfo.text = "Recipient : $receiverName - $receiverNum"

                                        if (pickupCoords == null || dropCoords == null) {
                                            Log.e("DeliveryStatus", "⚠️ pickup/drop coords are null!")
                                            return@addOnSuccessListener
                                        }

                                        getEstimatedTravelTime(pickupCoords!!, dropCoords!!) { eta, km ->
                                            durationTime.text = eta
                                            totalKilometer.text = "${String.format("%.1f", km)} km"

                                            val mapUrl =
                                                "https://farmnook-web.vercel.app/live-tracking?pickup=${pickupCoords!!.replace(" ", "")}&drop=${dropCoords!!.replace(" ", "")}&haulerId=$haulerId"
                                            Log.d("DeliveryStatus", "🌐 Loading map URL: $mapUrl")
                                            webView.loadUrl(mapUrl)
                                        }
                                    }
                                }
                    }
                    .addOnFailureListener {
                        Log.e("DeliveryStatus", "❌ Failed to fetch deliveryRequest: ${it.message}")
                    }
            }
    }

    //NEED IBALHIN
    private fun getEstimatedTravelTime(pickup: String, drop: String, callback: (String, Double) -> Unit) {
        val (startLat, startLng) = pickup.split(",")
        val (endLat, endLng) = drop.split(",")

        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$startLng,$startLat;$endLng,$endLat?access_token=$mapboxToken&overview=false"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Unknown", 0.0)
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val route = json.getJSONArray("routes").optJSONObject(0)
                    val duration = route?.getDouble("duration") ?: 0.0
                    val distance = route?.getDouble("distance") ?: 0.0

                    val mins = (duration / 60).toInt()
                    val eta = if (mins < 60) "$mins min" else "${mins / 60} hr ${mins % 60} min"
                    Handler(Looper.getMainLooper()).post { callback(eta, distance / 1000) }
                }
            })
    }

    private fun checkPermissionsAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            setupLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLocation = location
                injectLocationToWebView(location.latitude, location.longitude)
                deliveryId?.let { checkProximityAndUpdate(it, location) }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun showCompletionConfirmation(deliveryId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delivery Completion")
            .setMessage("Are you sure you've completed delivering the products?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                completeDelivery(deliveryId)
            }
            .setNegativeButton("No") { dlg, _ -> dlg.dismiss() }
            .create()
            .show()
    }

    private fun completeDelivery(deliveryId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val deliveryRef = firestore.collection("deliveries").document(deliveryId)

        deliveryRef.update("isDone", true)
            .addOnSuccessListener {
                val historyRef = firestore.collection("deliveryHistory").document()
                val completedAt = Timestamp.now()
                val history = DeliveryHistory(historyRef.id, deliveryId, completedAt, "N/A")

                historyRef.set(history).addOnSuccessListener {
                    deliveryRef.get().addOnSuccessListener { deliveryDoc ->
                        val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                        val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                        firestore.collection("deliveryRequests").document(requestId).get()
                            .addOnSuccessListener { reqDoc ->
                                val farmerId =
                                    reqDoc.getString("farmerId") ?: return@addOnSuccessListener
                                val businessId =
                                    reqDoc.getString("businessId") ?: return@addOnSuccessListener
                                val estimatedTime = reqDoc.getString("estimatedTime") ?: "N/A"
                                val pickupAddress = reqDoc.getString("pickupName") ?: "Unknown"
                                val destinationAddress = reqDoc.getString("destinationName") ?: "Unknown"
                                val receiverName = reqDoc.getString("receiverName") ?: "Unknown"
                                val receiverNum = reqDoc.getString("receiverNumber") ?: "Unknown"

                                val chatId =
                                    if (haulerId < farmerId) "$haulerId-$farmerId" else "$farmerId-$haulerId"

                                firestore.collection("users").document(farmerId).get()
                                    .addOnSuccessListener { farmerDoc ->
                                        val farmerName =
                                            "${farmerDoc.getString("firstName") ?: ""} ${
                                                farmerDoc.getString("lastName") ?: ""
                                            }".trim()
                                        val profileImage =
                                            farmerDoc.getString("profileImageUrl") ?: "N/A"

                                        firestore.collection("users").document(haulerId).get()
                                            .addOnSuccessListener { haulerDoc ->
                                                val fullName =
                                                    "${haulerDoc.getString("firstName") ?: ""} ${
                                                        haulerDoc.getString("lastName") ?: ""
                                                    }".trim()
                                                val title = "Delivery Completed"
                                                val message =
                                                    "$fullName has completed the delivery."
                                                val nowMillis = completedAt.toDate().time

                                                SendPushNotification.sendCompletedDeliveryNotification(
                                                    "recipientId", farmerId,
                                                    deliveryId, title, message,
                                                    completedAt, requireContext()
                                                )
                                                SendPushNotification.sendCompletedDeliveryNotification(
                                                    "businessId", businessId,
                                                    deliveryId, title, message,
                                                    completedAt, requireContext()
                                                )

                                                val chatRef =
                                                    firestore.collection("chats").document(chatId)
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

                                                // Stop the location service
                                                requireActivity().stopService(
                                                    Intent(
                                                        requireContext(),
                                                        DeliveryLocationService::class.java
                                                    )
                                                )

                                                // Navigate to history details
                                                Intent(
                                                    requireContext(),
                                                    HistoryDetailsActivity::class.java
                                                ).apply {
                                                    putExtra("deliveryId", deliveryId)
                                                    putExtra("pickup", pickupCoords)
                                                    putExtra("destination", dropCoords)
                                                    putExtra("pickupAddress", pickupAddress)
                                                    putExtra(
                                                        "destinationAddress",
                                                        destinationAddress
                                                    )
                                                    putExtra("estimatedTime", estimatedTime)
                                                    putExtra("farmerName", farmerName)
                                                    putExtra("profileImg", profileImage)
                                                }.also { startActivity(it) }
                                                requireActivity().finish()
                                            }
                                    }
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to complete delivery: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun checkProximityAndUpdate(deliveryId: String, location: Location) {
        val deliveryRef = firestore.collection("deliveries").document(deliveryId)

        if (pickupCoords == null || dropCoords == null) {
            Log.d("DeliveryStatus", "pickupCoords or dropCoords is null")
            return
        }

        val (pickupLat, pickupLng) = pickupCoords!!.split(",").map { it.toDouble() }
        val (dropLat, dropLng) = dropCoords!!.split(",").map { it.toDouble() }

        val pickupLoc = Location("").apply { latitude = pickupLat; longitude = pickupLng }
        val dropLoc = Location("").apply { latitude = dropLat; longitude = dropLng }
        val doneDeliveryBtn = view?.findViewById<Button>(R.id.doneDeliveryBtn)

        Log.d("DeliveryStatus", "Distance to drop: ${location.distanceTo(dropLoc)} meters")

        deliveryRef.get().addOnSuccessListener { doc ->
            val atPickup = doc.getBoolean("arrivedAtPickup") ?: false
            val atDestination = doc.getBoolean("arrivedAtDestination") ?: false
            val isDone = doc.getBoolean("isDone") ?: false

            Log.d("DeliveryStatus", "Current state - atPickup: $atPickup, atDestination: $atDestination, isDone: $isDone")
            Log.d("DeliveryStatus", "Button visibility before update: ${doneDeliveryBtn?.visibility}")

            when {
                !atPickup && location.distanceTo(pickupLoc) <= 20 -> {
                    Log.d("DeliveryStatus", "Arrived at pickup point")
                    deliveryRef.update("arrivedAtPickup", true)
                    currentStatus = "On the Way"
                }
                atPickup && !atDestination && location.distanceTo(dropLoc) <= 20 -> {
                    Log.d("DeliveryStatus", "Arrived at destination point")
                    deliveryRef.update("arrivedAtDestination", true)
                    currentStatus = "Arrived at Destination"
                    doneDeliveryBtn?.let { button ->
                        Log.d("DeliveryStatus", "Setting button visibility to VISIBLE")
                        button.visibility = View.VISIBLE
                        Log.d("DeliveryStatus", "Button visibility after update: ${button.visibility}")
                    }
                }
            }

            injectStatusToWebView()
        }
    }

    private fun injectLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) return

        firestore.collection("haulerLocations").document(haulerId)
            .set(mapOf("latitude" to lat, "longitude" to lng, "timestamp" to System.currentTimeMillis()))

        webView.evaluateJavascript("window.updateUserLocation($lat, $lng);", null)
    }

    private fun injectStatusToWebView() {
        val js = "window.updateRouteStatus('$currentStatus');"
        webView.evaluateJavascript(js) { Log.d("STATUS_INJECT", it ?: "null") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
