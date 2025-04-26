package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.ui.adapter.RecommendationAdapter
import com.ucb.capstone.farmnook.ui.farmer.WaitingDeliveryActivity
import kotlin.math.pow

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecommendationAdapter
    private val vehicleList = mutableListOf<VehicleWithBusiness>()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var pickupLocation: String
    private lateinit var destinationLocation: String
    private lateinit var purpose: String
    private lateinit var productType: String
    private lateinit var weight: String
    private lateinit var recommendedTypes: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recommendedTypes = intent.getStringArrayListExtra("recommendedTypes") ?: listOf()

        recyclerView = findViewById(R.id.recommended_haulers_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecommendationAdapter(vehicleList, ::onAvailableClicked)
        recyclerView.adapter = adapter

        pickupLocation = intent.getStringExtra("pickupLocation") ?: ""
        destinationLocation = intent.getStringExtra("destinationLocation") ?: ""
        purpose = intent.getStringExtra("purpose") ?: ""
        productType = intent.getStringExtra("productType") ?: ""
        weight = intent.getStringExtra("weight") ?: ""

        //filter (Location, Ratings)
        val filterSpinner: Spinner = findViewById(R.id.filter_spinner)
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { // Location
                        sortByLocation()
                    }
                    1 -> { // Ratings
                        sortByRatings()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
        fetchVehiclesWithBusinessInfo()

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchVehiclesWithBusinessInfo() {
        val pickupCoords = pickupLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        if (pickupCoords.size != 2) {
            Toast.makeText(this, "Invalid pickup location.", Toast.LENGTH_SHORT).show()
            return
        }

        val pickupLat = pickupCoords[0]
        val pickupLng = pickupCoords[1]

        firestore.collection("vehicles")
            .get()
            .addOnSuccessListener { vehiclesSnapshot ->
                val vehicles = vehiclesSnapshot.documents
                    .filter { it.getString("vehicleType")?.let { type -> recommendedTypes.contains(type) } == true }

                val businessIds = vehicles.mapNotNull { it.getString("businessId") }.toSet()

                firestore.collection("users")
                    .whereIn("userId", businessIds.toList())
                    .get()
                    .addOnSuccessListener { usersSnapshot ->

                        //fetch data
                        val businessMap = usersSnapshot.documents.associateBy(
                            { it.getString("userId") ?: "" },
                            {  val avgRating = when (val avgField = it.get("averageRating")) {
                                is Number -> avgField.toDouble()
                                is String -> avgField.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                                Triple(
                                    it.getString("businessName") ?: "Unknown Business",
                                    it.getString("location") ?: "",
                                    it.getString("profileImageUrl")
                                ) to avgRating }
                        )

                        val vehicleWithDistance = mutableListOf<Pair<VehicleWithBusiness, Double>>()

                        for (doc in vehicles) {
                            val busId = doc.getString("businessId") ?: continue
                            val (businessInfo, avgRating) = businessMap[busId] ?: continue
                            val (businessName, locationStr, profileImage) = businessInfo

                            val locationCoords = locationStr.split(",")
                            if (locationCoords.size != 2) continue

                            val lat = locationCoords[0].toDoubleOrNull() ?: continue
                            val lng = locationCoords[1].toDoubleOrNull() ?: continue
                            val distance = haversine(pickupLat, pickupLng, lat, lng)

                            val vehicleObj = VehicleWithBusiness(
                                vehicleId = doc.id,
                                vehicleType = doc.getString("vehicleType") ?: "Unknown",
                                model = doc.getString("model") ?: "Unknown Model",
                                plateNumber = doc.getString("plateNumber") ?: "Unknown Plate",
                                businessName = businessName,
                                businessId = busId,
                                businessLocation = locationStr,
                                profileImage = profileImage,
                                averageRating = avgRating
                            )

                            vehicleWithDistance.add(Pair(vehicleObj, distance))
                        }

                        vehicleList.clear()
                        vehicleList.addAll(vehicleWithDistance.map { it.first })
                        sortByLocation()
                        adapter.notifyDataSetChanged()

                        if (vehicleList.isEmpty()) {
                            Toast.makeText(this, "No matching vehicles found for '$recommendedTypes'", Toast.LENGTH_LONG).show()
                        }
                    }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortByLocation() {
        val pickupCoords = pickupLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        if (pickupCoords.size != 2) return

        val pickupLat = pickupCoords[0]
        val pickupLng = pickupCoords[1]

        vehicleList.sortBy { vehicle ->
            val locationCoords = vehicle.businessLocation.split(",")
            if (locationCoords.size != 2) Double.MAX_VALUE
            else {
                val lat = locationCoords[0].toDoubleOrNull() ?: Double.MAX_VALUE
                val lng = locationCoords[1].toDoubleOrNull() ?: Double.MAX_VALUE
                haversine(pickupLat, pickupLng, lat, lng)
            }
        }
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortByRatings() {
        vehicleList.sortByDescending { it.averageRating ?: 0.0 }
        adapter.notifyDataSetChanged()
    }

    //Finding nearest
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3 // Radius of the Earth in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun onAvailableClicked(vehicle: VehicleWithBusiness) {
        val farmerId = intent.getStringExtra("farmerId") ?: ""
        val delivery = DeliveryRequest(
            pickupLocation = pickupLocation,
            destinationLocation = destinationLocation,
            purpose = purpose,
            productType = productType,
            weight = weight,
            farmerId = farmerId,
            timestamp = Timestamp.now(),
            vehicleID = vehicle.vehicleId,
            businessId = vehicle.businessId,
            isAccepted = false
        )

        val dialog = DeliverySummaryDialogFragment.newInstance(vehicle, delivery) {
            saveToDeliveryRequests(it.first, it.second) { generatedRequestId ->
                // ✅ This runs after saving to Firestore
                val intent = Intent(this, WaitingDeliveryActivity::class.java)
                intent.putExtra("requestId", generatedRequestId)
                startActivity(intent)
            }
        }

        dialog.show(supportFragmentManager, "DeliverySummary")
    }

    private fun saveToDeliveryRequests(vehicle: VehicleWithBusiness, delivery: DeliveryRequest,  onSuccess: (String) -> Unit ) {
        val deliveryRequestsRef = firestore.collection("deliveryRequests")
        val newRequestRef = deliveryRequestsRef.document()
        val requestId = newRequestRef.id

        val requestData = hashMapOf(
            "requestId" to requestId, // Store the generated ID
            "vehicleId" to vehicle.vehicleId,
            "businessId" to vehicle.businessId,
            "pickupLocation" to delivery.pickupLocation,
            "destinationLocation" to delivery.destinationLocation,
            "purpose" to delivery.purpose,
            "productType" to delivery.productType,
            "weight" to delivery.weight,
            "farmerId" to delivery.farmerId,
            "timestamp" to delivery.timestamp,
            "isAccepted" to delivery.isAccepted
        )

        newRequestRef.set(requestData)
            .addOnSuccessListener {

                onSuccess(requestId) // ✅ Call this callback after success

                val farmerId = delivery.farmerId
                if (farmerId != null) {
                    FirebaseFirestore.getInstance().collection("users").document(farmerId)
                        .get()
                        .addOnSuccessListener { farmerDoc ->
                            val farmerName = farmerDoc.getString("firstName") + " " + farmerDoc.getString("lastName")
                            delivery.businessId?.let { it1 ->
                                SendPushNotification.notifyBusinessOfRequest(
                                    context = this,
                                    businessId = it1,
                                    farmerName = farmerName ?: "A farmer"
                                )
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show()
            }
    }
}