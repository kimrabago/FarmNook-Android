package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Delivery
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.ui.adapter.RecommendationAdapter

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecommendationAdapter
    private val vehicleList = mutableListOf<VehicleWithBusiness>()
    private val firestore = FirebaseFirestore.getInstance()

    // Delivery fields passed from AddDeliveryActivity
    private lateinit var pickupLocation: String
    private lateinit var destinationLocation: String
    private lateinit var purpose: String
    private lateinit var productType: String
    private lateinit var weight: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recyclerView = findViewById(R.id.recommended_haulers_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RecommendationAdapter(vehicleList, ::onAvailableClicked)
        recyclerView.adapter = adapter

        // Extract delivery data from intent
        pickupLocation = intent.getStringExtra("pickupLocation") ?: ""
        destinationLocation = intent.getStringExtra("destinationLocation") ?: ""
        purpose = intent.getStringExtra("purpose") ?: ""
        productType = intent.getStringExtra("productType") ?: ""
        weight = intent.getStringExtra("weight") ?: ""

        fetchVehiclesWithBusinessInfo()

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchVehiclesWithBusinessInfo() {
        firestore.collection("vehicles")
            .get()
            .addOnSuccessListener { vehiclesSnapshot ->
                val vehicles = vehiclesSnapshot.documents
                val businessIds = vehicles.mapNotNull { it.getString("businessId") }.toSet()

                firestore.collection("users")
                    .whereIn("userId", businessIds.toList())
                    .get()
                    .addOnSuccessListener { usersSnapshot ->
                        val businessMap = usersSnapshot.documents.associateBy(
                            { it.getString("userId") ?: "" },
                            { it.getString("businessName") ?: "Unknown Business" }
                        )

                        vehicleList.clear()
                        for (doc in vehicles) {
                            val busId = doc.getString("businessId") ?: continue
                            val businessName = businessMap[busId] ?: "Unknown Business"
                            vehicleList.add(
                                VehicleWithBusiness(
                                    vehicleId = doc.id,
                                    model = doc.getString("model") ?: "Unknown Model",
                                    plateNumber = doc.getString("plateNumber") ?: "Unknown Plate",
                                    maxWeightKg = doc.getLong("maxWeightKg")?.toInt() ?: 0,
                                    size = doc.getString("size") ?: "N/A",
                                    businessName = businessName,
                                    businessId = busId
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }
            }
    }

    private fun onAvailableClicked(vehicle: VehicleWithBusiness) {
        val farmerId = intent.getStringExtra("farmerId") ?: ""
        val delivery = Delivery(
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
            saveToDeliveryRequests(it.first, it.second)
        }
        dialog.show(supportFragmentManager, "DeliverySummary")
    }

    private fun saveToDeliveryRequests(vehicle: VehicleWithBusiness, delivery: Delivery) {
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
            "farmerId" to delivery.farmerId,
            "timestamp" to delivery.timestamp,
            "isAccepted" to delivery.isAccepted
        )

        newRequestRef.set(requestData)
            .addOnSuccessListener {
                Toast.makeText(this, "Delivery request sent!", Toast.LENGTH_SHORT).show()

                val farmerId = delivery.farmerId
                if (farmerId != null) {
                    FirebaseFirestore.getInstance().collection("users").document(farmerId)
                        .get()
                        .addOnSuccessListener { farmerDoc ->
                            val farmerName = farmerDoc.getString("firstName") + " " + farmerDoc.getString("lastName")
                            delivery.businessId?.let { it1 ->
                                DeliveryRequestSendNotification.notifyBusinessOfRequest(
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