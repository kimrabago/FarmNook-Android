package com.ucb.capstone.farmnook.ui.users.farmer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.*
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.util.getAddressFromLatLng
import com.ucb.capstone.farmnook.utils.loadImage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.location.Location
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.ucb.capstone.farmnook.ui.message.MessageActivity

class FarmerDeliveryStatusFragment : Fragment(R.layout.fragment_farmer_delivery_status) {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loadingLayout: View
    private lateinit var confirmationLayout: View
    private lateinit var haulerToPickupLayout: View
    private lateinit var haulerArrivalLayout: View
    private lateinit var haulerOnDeliveryLayout: View
    private lateinit var haulerArrivedAtDestinationLayout: View
    private lateinit var noActiveDeliveryLayout: View
    private lateinit var webView: WebView
    private lateinit var geocoder: Geocoder

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


    private lateinit var vehicleWtBusiness: VehicleWithBusiness
    private lateinit var deliveryReq: DeliveryRequest

    private lateinit var haulerProfileImage: CircleImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val messageIcon = view.findViewById<ImageButton>(R.id.messageIcon)
        messageIcon.setOnClickListener {
            val deliveryId = this.deliveryId ?: return@setOnClickListener


            FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId).get()
                .addOnSuccessListener { deliveryDoc ->
                    val farmerId = FirebaseAuth.getInstance().currentUser?.uid
                        ?: return@addOnSuccessListener
                    val haulerId = deliveryDoc.getString("haulerAssignedId")
                        ?: return@addOnSuccessListener


                    // Generate chat ID (must match format used elsewhere)
                    val chatId = if (farmerId < haulerId) "$farmerId-$haulerId"
                    else "$haulerId-$farmerId"


                    // Fetch hauler details for the chat
                    FirebaseFirestore.getInstance().collection("users").document(haulerId).get()
                        .addOnSuccessListener { haulerDoc ->
                            val firstName = haulerDoc.getString("firstName") ?: ""
                            val lastName = haulerDoc.getString("lastName") ?: ""
                            val haulerName = "$firstName $lastName".trim()


                            Intent(requireContext(), MessageActivity::class.java).apply {
                                putExtra("chatId", chatId)
                                putExtra("recipientId", haulerId)
                                putExtra("receiverName", haulerName)
                            }.also { startActivity(it) }
                        }
                        .addOnFailureListener {
                            // Start chat even if name fetch fails
                            Intent(requireContext(), MessageActivity::class.java).apply {
                                putExtra("chatId", chatId)
                                putExtra("recipientId", haulerId)
                            }.also { startActivity(it) }
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(),
                        "Failed to load delivery details",
                        Toast.LENGTH_SHORT).show()
                }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        webView = view.findViewById(R.id.mapView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = WebViewClient()

        haulerProfileImage = view.findViewById(R.id.profileImage)
        loadingLayout = view.findViewById(R.id.loadingLayout)
        confirmationLayout = view.findViewById(R.id.confirmationLayout)
        haulerToPickupLayout = view.findViewById(R.id.haulerToPickupLayout)
        haulerArrivalLayout = view.findViewById(R.id.haulerArrival)
        haulerOnDeliveryLayout = view.findViewById(R.id.haulerOnDelivery)
        haulerArrivedAtDestinationLayout = view.findViewById(R.id.haulerArrivedAtDestination)
        noActiveDeliveryLayout = view.findViewById(R.id.noActiveDeliveryLayout)

        geocoder = Geocoder(requireContext(), Locale.getDefault())
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        cancelButton.setOnClickListener { cancelDeliveryRequest() }

        val detailsButton = view.findViewById<Button>(R.id.deliveryDetailsBtn)
        detailsButton.setOnClickListener {
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

        val requestId = requireArguments().getString("requestId")
        argPickupName = requireArguments().getString("pickupName")
        argDestinationName = requireArguments().getString("destinationName")
        argPurpose = requireArguments().getString("purpose")
        argProductType = requireArguments().getString("productType")
        argWeight = requireArguments().getString("weight")
        argBusinessName = requireArguments().getString("businessId")
        argTotalCost = requireArguments().getDouble("estimatedCost", -1.0)
        argEstimatedTime = requireArguments().getString("estimatedTime")
        argBusinessName = requireArguments().getString("businessName")
        argLocationName = requireArguments().getString("locationName")
        argProfileImage = requireArguments().getString("profileImageUrl")
        argVehicleType = requireArguments().getString("vehicleType")
        argVehicleModel = requireArguments().getString("vehicleModel")
        argPlateNumber = requireArguments().getString("plateNumber")

        if (requestId.isNullOrEmpty()) {
            showNoActiveDelivery()
            return
        }

        deliveryRequestRef =
            FirebaseFirestore.getInstance().collection("deliveryRequests").document(requestId)

        deliveryRequestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                showNoActiveDelivery()
                return@addOnSuccessListener
            }

            val pickup = doc.getString("pickupLocation") ?: ""
            val destination = doc.getString("destinationLocation") ?: ""

            if (pickup.isNotEmpty() && destination.isNotEmpty()) {
                val pickupLocName = argPickupName ?: getAddressFromLatLng(pickup, geocoder)
                val destLocName = argDestinationName ?: getAddressFromLatLng(destination, geocoder)

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
                Toast.makeText(requireContext(), "Missing pickup/destination", Toast.LENGTH_SHORT)
                    .show()
                showNoActiveDelivery()
            }
        }

        listenForDeliveryConfirmation()
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
                    requireContext(),
                    "Failed to get delivery details",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun listenToDeliveryStatus(deliveryId: String) {
        val deliveryRef =
            FirebaseFirestore.getInstance().collection("deliveries").document(deliveryId)
        listenerRegistration = deliveryRef.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

            val isDone = snapshot.getBoolean("isDone") ?: false
            val arrivedAtDestination = snapshot.getBoolean("arrivedAtDestination") ?: false
            val arrivedAtPickup = snapshot.getBoolean("arrivedAtPickup") ?: false
            val isStarted = snapshot.getBoolean("isStarted")?: false

            when {
                isDone -> showCompletedMessage()
                arrivedAtDestination -> showArrivedAtDestinationLayout()
                arrivedAtPickup -> showArrivalLayout()
                isStarted -> showEnRouteLayout()
                else -> showNoActiveDelivery()
            }
        }
    }

    private fun listenForDeliveryConfirmation() {
        deliveryRequestRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(
                    requireContext(),
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

    @SuppressLint("SetTextI18n")
    private fun fetchDeliveryDetails(requestId: String) {
        val deliveriesRef = FirebaseFirestore.getInstance()
            .collection("deliveries")
            .whereEqualTo("requestId", requestId)

        deliveriesRef.get().addOnSuccessListener { querySnapshot ->
            if (!querySnapshot.isEmpty) {
                val deliveryDoc = querySnapshot.documents[0]
                val haulerId =
                    deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener
                val deliveryId = deliveryDoc.getString("deliveryId") ?: return@addOnSuccessListener

                val deliveryIdTextView = view?.findViewById<TextView>(R.id.deliveryId)
                deliveryIdTextView?.text = "Delivery ID: $deliveryId"

                // Fetch deliveryRequest data to build DeliveryRequest object
                deliveryRequestRef.get().addOnSuccessListener { requestDoc ->
                    val vehicleId = requestDoc.getString("vehicleId") ?: return@addOnSuccessListener
                    val cost = requestDoc.getDouble("estimatedCost") ?: return@addOnSuccessListener
                    val estimatedTime =
                        requestDoc.getString("estimatedTime") ?: return@addOnSuccessListener
                    val pickup = requestDoc.getString("pickupLocation") ?: ""
                    val drop = requestDoc.getString("destinationLocation") ?: ""
                    val pickupLocation = getAddressFromLatLng(pickup, geocoder)
                    val destinationLocation = getAddressFromLatLng(drop, geocoder)


                    // Fetch vehicle details and assign to `vehicleWtBusiness`
                    fetchVehicleDetails(vehicleId) { fetchedVehicleWithBusiness ->
                        this.vehicleWtBusiness = fetchedVehicleWithBusiness

                        // Now safe to fetch hauler and update UI
                        fetchHaulerDetails(haulerId)
                    }
                }

            } else {
                Log.w("DeliveryStatusFragment", "No delivery found for requestId: $requestId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching delivery details: ${e.message}")
        }
    }

    private fun fetchHaulerDetails(haulerId: String) {
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(haulerId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val haulerID = document.getString("userId")
                val haulerName =
                    document.getString("firstName") + " " + document.getString("lastName")
                val haulerProfileImg = document.getString("profileImageUrl")

                val haulerNameTextView = view?.findViewById<TextView>(R.id.haulerName)

                haulerNameTextView?.text = haulerName

                haulerProfileImage.loadImage(haulerProfileImg)

                showConfirmationLayout()
            } else {
                Log.w("DeliveryStatusFragment", "Hauler not found with ID: $haulerId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching hauler details: ${e.message}")
        }
    }

    // added
    @SuppressLint("SetTextI18n")
    private fun fetchVehicleDetails(vehicleId: String, callback: (VehicleWithBusiness) -> Unit) {
        val vehicleRef = FirebaseFirestore.getInstance()
            .collection("vehicles")
            .document(vehicleId)

        vehicleRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val vehicleType = document.getString("vehicleType") ?: "Unknown Type"
                val model = document.getString("model") ?: "Unknown Model"
                val plateNumber = document.getString("plateNumber") ?: "Unknown Plate"
                val businessId = document.getString("businessId") ?: "Unknown"

                val vehicleTypeTextView = view?.findViewById<TextView>(R.id.vehicleType)
                val plateNumberTextView = view?.findViewById<TextView>(R.id.plateNumber)

                vehicleTypeTextView?.text = "$vehicleType - $model"
                plateNumberTextView?.text = plateNumber


            } else {
                Log.w("DeliveryStatusFragment", "Vehicle not found with ID: $vehicleId")
            }
        }.addOnFailureListener { e ->
            Log.e("DeliveryStatusFragment", "Error fetching vehicle details: ${e.message}")
        }
    }

    private fun cancelDeliveryRequest() {
        deliveryRequestRef.update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Delivery request cancelled successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                val navBar = activity as? NavigationBar
                navBar?.let { nav ->
                    nav.restoreActiveRequestId {
                        if (isAdded) {
                            requireActivity().runOnUiThread {
                                nav.resetToDashboard()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to cancel delivery request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

    private fun showLoadingLayout() {
        loadingLayout.visibility = View.VISIBLE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE
    }

    private fun showConfirmationLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.VISIBLE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE
    }

    private fun showEnRouteLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.VISIBLE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE
    }

    private fun showArrivalLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.VISIBLE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE
    }
    private fun showHaulerOnDelivery() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.VISIBLE
        haulerArrivedAtDestinationLayout.visibility = View.GONE
    }
    private fun showArrivedAtDestinationLayout() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.GONE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.VISIBLE
    }

    private fun showCompletedMessage() {
        Toast.makeText(requireContext(), "✅ Delivery completed!", Toast.LENGTH_LONG).show()
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE

        val navBar = activity as? NavigationBar
        navBar?.let { nav ->
            nav.activeRequestId = null // ✅ Clear the active request
            nav.restoreActiveRequestId {
                if (isAdded) {
                    requireActivity().runOnUiThread {
                        nav.resetToDashboard() // ✅ Back to dashboard to allow new request
                    }
                }
            }
        }
    }

    private fun showNoActiveDelivery() {
        loadingLayout.visibility = View.GONE
        confirmationLayout.visibility = View.GONE
        haulerToPickupLayout.visibility = View.GONE
        haulerArrivalLayout.visibility = View.GONE
        noActiveDeliveryLayout.visibility = View.VISIBLE
        haulerOnDeliveryLayout.visibility = View.GONE
        haulerArrivedAtDestinationLayout.visibility = View.GONE

        // Only show this gray fallback when no delivery exists
        webView.setBackgroundColor(Color.parseColor("#E0E0E0"))
        webView.loadData(
            "<html><body style='background-color:#E0E0E0;'><h3 style='color:#888; text-align:center; margin-top:50%;'>No Active Delivery</h3></body></html>",
            "text/html", "utf-8"
        )
    }

}