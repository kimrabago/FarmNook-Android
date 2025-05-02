package com.ucb.capstone.farmnook.ui.farmer.add_delivery

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
import com.ucb.capstone.farmnook.utils.EstimateTravelTimeUtil
import com.ucb.capstone.farmnook.utils.RetrofitClient
import com.ucb.capstone.farmnook.utils.SendPushNotification
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

        val filterSpinner: Spinner = findViewById(R.id.filter_spinner)
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> sortByLocation()
                    1 -> sortByRatings()
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
                                it.getString("profileImageUrl")
                            ) to avgRating
                        }
                    )

                    val apiService = RetrofitClient.instance?.create(ApiService::class.java)

                    vehicles.forEach { doc ->
                        val busId = doc.getString("businessId") ?: return@forEach
                        val (businessInfo, avgRating) = businessMap[busId] ?: return@forEach
                        val (businessName, locationStr, profileImage) = businessInfo

                        val locationCoords = locationStr.split(",")
                        if (locationCoords.size != 2) return@forEach

                        val lat = locationCoords[0].toDoubleOrNull() ?: return@forEach
                        val lng = locationCoords[1].toDoubleOrNull() ?: return@forEach

                        val pickupDistance = haversine(lat, lng, pickupLat, pickupLng)
                        val deliveryDistance = haversine(pickupLat, pickupLng, destinationLat, destinationLng)

                        // 👇 Convert meters to kilometers and log
                        Log.d("DISTANCE_DEBUG", "Pickup Distance: ${pickupDistance / 1000.0} km")
                        Log.d("DISTANCE_DEBUG", "Delivery Distance: ${deliveryDistance / 1000.0} km")

                        val vehicleObj = VehicleWithBusiness(
                            vehicleId = doc.id,
                            vehicleType = doc.getString("vehicleType") ?: "Unknown",
                            model = doc.getString("model") ?: "Unknown Model",
                            plateNumber = doc.getString("plateNumber") ?: "Unknown Plate",
                            businessName = businessName,
                            businessId = busId,
                            businessLocation = locationStr,
                            profileImage = profileImage,
                            averageRating = avgRating,
                            estimatedCost = null
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
                                adapter.notifyDataSetChanged()
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

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
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

        lifecycleScope.launch {
            val estimatedTime =
                EstimateTravelTimeUtil.getEstimatedTravelTime(pickupLocation, destinationLocation)

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
                isAccepted = false,
                estimatedCost = vehicle.estimatedCost,
                estimatedTime = estimatedTime
            )

            val dialog = DeliverySummaryDialogFragment.newInstance(vehicle, delivery) {
                saveToDeliveryRequests(it.first, it.second) { generatedRequestId ->
                    val intent =
                        Intent(this@RecommendationActivity, NavigationBar::class.java).apply {
                            putExtra("navigateTo", "DeliveryStatus")
                            putExtra("requestId", generatedRequestId)
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
            "destinationLocation" to delivery.destinationLocation,
            "purpose" to delivery.purpose,
            "productType" to delivery.productType,
            "weight" to delivery.weight,
            "farmerId" to delivery.farmerId,
            "timestamp" to delivery.timestamp,
            "isAccepted" to delivery.isAccepted,
            "estimatedCost" to delivery.estimatedCost,
            "estimatedTime" to delivery.estimatedTime
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
