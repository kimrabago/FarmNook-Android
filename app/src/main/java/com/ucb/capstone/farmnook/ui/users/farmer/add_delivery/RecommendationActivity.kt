package com.ucb.capstone.farmnook.ui.users.farmer.add_delivery

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.data.model.algo.CostEstimationRequest
import com.ucb.capstone.farmnook.data.model.algo.CostEstimationResponse
import com.ucb.capstone.farmnook.data.service.ApiService
import com.ucb.capstone.farmnook.ui.adapter.RecommendationAdapter
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.utils.CombineTimeDurations
import com.ucb.capstone.farmnook.utils.EstimateTravelTimeUtil
import com.ucb.capstone.farmnook.utils.RetrofitClient
import com.ucb.capstone.farmnook.utils.SendPushNotification
import com.ucb.capstone.farmnook.utils.GeoUtils.haversine
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecommendationAdapter
    private val vehicleList = mutableListOf<VehicleWithBusiness>()
    private val originalVehicleList = mutableListOf<VehicleWithBusiness>()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var pickupLocation: String
    private lateinit var destinationLocation: String
    private lateinit var pickupName: String
    private lateinit var destinationName: String
    private lateinit var purpose: String
    private lateinit var productType: String
    private lateinit var weight: String
    private lateinit var receiverName: String
    private lateinit var receiverNum: String
    private var deliveryNote: String? = null
    private lateinit var recommendedTypes: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recommendedTypes = intent.getStringArrayListExtra("recommendedTypes") ?: listOf()

        recyclerView = findViewById(R.id.recommended_haulers_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecommendationAdapter(vehicleList, ::onAvailableClicked,  filterMode = 0)
        recyclerView.adapter = adapter

        pickupLocation = intent.getStringExtra("pickupLocation") ?: ""
        destinationLocation = intent.getStringExtra("destinationLocation") ?: ""
        purpose = intent.getStringExtra("purpose") ?: ""
        productType = intent.getStringExtra("productType") ?: ""
        weight = intent.getStringExtra("weight") ?: ""
        receiverName = intent.getStringExtra("receiverName") ?: ""
        receiverNum = intent.getStringExtra("receiverNum") ?: ""
        deliveryNote = intent.getStringExtra("deliveryNote") ?: ""
        pickupName = intent.getStringExtra("pickupName") ?: ""
        destinationName = intent.getStringExtra("destinationName") ?: ""

        val filterSpinner: Spinner = findViewById(R.id.filter_spinner)
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                adapter.filterMode = position
                when (position) {
                    0 -> sortByCombinedScore()
                    1 -> sortByEstimatedCost()
                    2 -> sortByLocation()
                    3 -> sortByRatings()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        fetchVehiclesWithBusinessInfo()

        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchVehiclesWithBusinessInfo() {
        val pickupCoords = pickupLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        val destinationCoords = destinationLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        if (pickupCoords.size != 2 || destinationCoords.size != 2) {
            Toast.makeText(this, "Invalid pickup or destination location.", Toast.LENGTH_SHORT).show()
            return
        }

        val pickupLat = pickupCoords[0]
        val pickupLng = pickupCoords[1]
        val destinationLat = destinationCoords[0]
        val destinationLng = destinationCoords[1]

        firestore.collection("vehicles").get().addOnSuccessListener { vehiclesSnapshot ->
            val vehicles = vehiclesSnapshot.documents.filter {
                it.getString("vehicleType")?.let { type -> recommendedTypes.contains(type) } == true
            }

            val businessIds = vehicles.mapNotNull { it.getString("businessId") }.toSet()

            firestore.collection("users")
                .whereIn("userId", businessIds.toList())
                .get()
                .addOnSuccessListener { usersSnapshot ->
                    val businessMap = usersSnapshot.documents.associateBy(
                        { it.getString("userId") ?: "" },
                        {
                            val avgRating = when (val avgField = it.get("averageRating")) {
                                is Number -> avgField.toDouble()
                                is String -> avgField.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                            Triple(
                                it.getString("businessName") ?: "Unknown Business",
                                it.getString("location") ?: "",
                                Pair(
                                    it.getString("profileImageUrl"),
                                    it.getString("locationName") ?: "Unknown Location"
                                )
                            ) to avgRating
                        }
                    )

                    val apiService = RetrofitClient.instance?.create(ApiService::class.java)

                    vehicles.forEach { vehicleDoc ->
                        val busId = vehicleDoc.getString("businessId") ?: return@forEach
                        val (businessInfo, avgRating) = businessMap[busId] ?: return@forEach
                        val (businessName, locationStr, profileImagePair) = businessInfo
                        val (profileImage, locationName) = profileImagePair

                        val locationCoords = locationStr.split(",")
                        if (locationCoords.size != 2) return@forEach

                        val businessLat = locationCoords[0].toDoubleOrNull() ?: return@forEach
                        val businessLng = locationCoords[1].toDoubleOrNull() ?: return@forEach

                        // Get the distance meters
                        val pickupDistance = haversine(businessLat, businessLng, pickupLat, pickupLng)
                        val deliveryDistance = haversine(pickupLat, pickupLng, destinationLat, destinationLng)

                        //
                        val vehicleObj = VehicleWithBusiness(
                            vehicleId = vehicleDoc.id,
                            vehicleType = vehicleDoc.getString("vehicleType") ?: "Unknown",
                            model = vehicleDoc.getString("model") ?: "Unknown Model",
                            plateNumber = vehicleDoc.getString("plateNumber") ?: "Unknown Plate",
                            businessName = businessName,
                            businessId = busId,
                            businessLocation = locationStr,
                            locationName = locationName,
                            profileImage = profileImage,
                            averageRating = avgRating,
                            estimatedCost = null,
                            pickupDistanceKm = pickupDistance / 1000.0
                        )

                        //COSTING REQUEST
                        val costRequest = CostEstimationRequest(
                            vehicleType = vehicleObj.vehicleType,
                            weight = weight.toDouble(),
                            pickupDistance = pickupDistance / 1000.0,
                            deliveryDistance = deliveryDistance / 1000.0
                        )

                        //COSTING RESPONSE
                        apiService?.estimateCost(costRequest)?.enqueue(object : Callback<CostEstimationResponse> {
                            override fun onResponse(call: Call<CostEstimationResponse>, response: Response<CostEstimationResponse>) {
                                Log.d("API_DEBUG", "Response: ${response.body()}")
                                if (response.isSuccessful) {
                                    val cost = response.body()?.estimatedCost
                                    vehicleObj.estimatedCost = cost
                                }
                                vehicleList.add(vehicleObj)
                                originalVehicleList.clear()
                                originalVehicleList.addAll(vehicleList)
                                sortByCombinedScore()
                            }

                            override fun onFailure(call: Call<CostEstimationResponse>, t: Throwable) {
                                Log.e("API_DEBUG", "Failed to estimate: ${t.message}", t)
                                vehicleList.add(vehicleObj)
                                adapter.notifyDataSetChanged()
                            }
                        })
                    }
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortByCombinedScore() {
        val pickupCoords = pickupLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        if (pickupCoords.size != 2) return

        val pickupLat = pickupCoords[0]
        val pickupLng = pickupCoords[1]

        vehicleList.sortByDescending { vehicle ->
            val locationCoords = vehicle.businessLocation.split(",")
            if (locationCoords.size != 2) return@sortByDescending Double.MIN_VALUE

            val businessLat = locationCoords[0].toDoubleOrNull() ?: return@sortByDescending Double.MIN_VALUE
            val businessLng = locationCoords[1].toDoubleOrNull() ?: return@sortByDescending Double.MIN_VALUE

            val distanceKm = haversine(pickupLat, pickupLng, businessLat, businessLng) / 1000.0
            val normalizedDistance = if (distanceKm > 0) 1 / distanceKm else 1.0

            val rating = vehicle.averageRating ?: 0.0
            val normalizedRating = rating / 5.0

            // Composite score: 70% weight to rating, 30% to proximity
            (0.7 * normalizedRating) + (0.3 * normalizedDistance)
        }

        adapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sortByEstimatedCost() {
        vehicleList.sortBy { it.estimatedCost ?: Double.MAX_VALUE }
        adapter.notifyDataSetChanged()
    }

    //find nearest location
    @SuppressLint("NotifyDataSetChanged")
    private fun sortByLocation() {
        val pickupCoords = pickupLocation.split(",").mapNotNull { it.toDoubleOrNull() }
        if (pickupCoords.size != 2) return

        val pickupLat = pickupCoords[0]
        val pickupLng = pickupCoords[1]

        vehicleList.sortBy {
            val locationCoords = it.businessLocation.split(",")
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

    private fun onAvailableClicked(vehicle: VehicleWithBusiness) {
        val farmerId = intent.getStringExtra("farmerId") ?: ""

        lifecycleScope.launch {
            val etaToPickup = EstimateTravelTimeUtil.getEstimatedTravelTime(vehicle.businessLocation, pickupLocation)
            val etaToDestination = EstimateTravelTimeUtil.getEstimatedTravelTime(pickupLocation, destinationLocation)

            val etaDropToBusinessLoc = EstimateTravelTimeUtil.getEstimatedTravelTime(destinationLocation, vehicle.businessLocation)

            val totalMinutes = CombineTimeDurations.parseMinutes(etaToPickup) + CombineTimeDurations.parseMinutes(etaToDestination)
            val estimatedTime = "$totalMinutes min"

            val overallEstTime = CombineTimeDurations.parseMinutes(etaToPickup) +
                    CombineTimeDurations.parseMinutes(etaToDestination) +
                    CombineTimeDurations.parseMinutes(etaDropToBusinessLoc) +
                    120


            val delivery = DeliveryRequest(
                pickupLocation = pickupLocation,
                pickupName = pickupName,
                destinationLocation = destinationLocation,
                destinationName = destinationName,
                purpose = purpose,
                productType = productType,
                weight = weight,
                farmerId = farmerId,
                timestamp = Timestamp.now(),
                vehicleId = vehicle.vehicleId,
                businessId = vehicle.businessId,
                isAccepted = false,
                estimatedCost = vehicle.estimatedCost,
                estimatedTime = estimatedTime,
                receiverName = receiverName,
                receiverNumber = receiverNum,
                deliveryNote = deliveryNote,
                etaToPickup = etaToPickup,
                etaToDestination = etaToDestination,
                overallEstimatedTime = overallEstTime,
                estimatedEndTime = null
            )

            val dialog = DeliverySummaryDialogFragment.newInstance(vehicle, delivery) {
                saveToDeliveryRequests(it.first, it.second) { generatedRequestId ->
                    val intent =
                        Intent(this@RecommendationActivity, NavigationBar::class.java).apply {
                            putExtra("navigateTo", "DeliveryStatus")
                            putExtra("requestId", generatedRequestId)
                            putExtra("pickupName", pickupName)
                            putExtra("destinationName", destinationName)
                            putExtra("purpose", purpose)
                            putExtra("productType", productType)
                            putExtra("weight", weight)
                            putExtra("receiverName", receiverName)
                            putExtra("receiverNum", receiverNum)
                            putExtra("weight", weight)
                            putExtra("estimatedCost", delivery.estimatedCost)
                            putExtra("estimatedTime", estimatedTime)
                            putExtra("businessId", vehicle.businessId)
                            putExtra("vehicleId", vehicle.vehicleId)
                            putExtra("businessName", vehicle.businessName)
                            putExtra("locationName", vehicle.locationName)
                            putExtra("profileImageUrl", vehicle.profileImage)
                            putExtra("vehicleID", vehicle.vehicleId)
                            putExtra("vehicleType", vehicle.vehicleType)
                            putExtra("vehicleModel", vehicle.model)
                            putExtra("plateNumber", vehicle.plateNumber)
                        }
                    startActivity(intent)
                    finish()
                }
            }

            dialog.show(supportFragmentManager, "DeliverySummary")
        }
    }

    private fun saveToDeliveryRequests(vehicle: VehicleWithBusiness, delivery: DeliveryRequest, onSuccess: (String) -> Unit) {
        val deliveryRequestsRef = firestore.collection("deliveryRequests")
        val newRequestRef = deliveryRequestsRef.document()
        val requestId = newRequestRef.id

        val requestData = hashMapOf(
            "requestId" to requestId,
            "vehicleId" to vehicle.vehicleId,
            "businessId" to vehicle.businessId,
            "pickupLocation" to delivery.pickupLocation,
            "pickupName" to pickupName,
            "destinationLocation" to delivery.destinationLocation,
            "destinationName" to destinationName,
            "purpose" to delivery.purpose,
            "productType" to delivery.productType,
            "weight" to delivery.weight,
            "farmerId" to delivery.farmerId,
            "timestamp" to delivery.timestamp,
            "isAccepted" to delivery.isAccepted,
            "estimatedCost" to delivery.estimatedCost,
            "estimatedTime" to delivery.estimatedTime,
            "etaToPickup" to delivery.etaToPickup,
            "etaToDestination" to delivery.etaToDestination,
            "overallEstimatedTime" to delivery.overallEstimatedTime,
            "estimatedEndTime" to delivery.estimatedEndTime,
            "receiverName" to delivery.receiverName,
            "receiverNumber" to delivery.receiverNumber,
            "deliveryNote" to delivery.deliveryNote,
            "scheduledTime" to delivery.scheduledTime,
        )

        newRequestRef.set(requestData).addOnSuccessListener {
            onSuccess(requestId)
            val farmerId = delivery.farmerId
            if (farmerId != null) {
                FirebaseFirestore.getInstance().collection("users").document(farmerId).get().addOnSuccessListener { farmerDoc ->
                    val farmerName = farmerDoc.getString("firstName") + " " + farmerDoc.getString("lastName")
                    delivery.businessId?.let {
                        SendPushNotification.notifyBusinessOfRequest(this, it, farmerName ?: "A farmer")
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show()
        }
    }
}
