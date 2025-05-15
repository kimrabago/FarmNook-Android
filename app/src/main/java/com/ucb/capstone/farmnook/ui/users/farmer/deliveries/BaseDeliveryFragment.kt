package com.ucb.capstone.farmnook.ui.users.farmer.deliveries

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.ui.adapter.DeliveriesAdapter
import com.ucb.capstone.farmnook.ui.users.farmer.FarmerDeliveryDetailsActivity
import java.text.SimpleDateFormat
import java.util.Locale

abstract class BaseDeliveryFragment : Fragment(R.layout.fragment_farm_delivery_list) {

    private var sharedVehicleType: String? = null
    private var sharedVehicleModel: String? = null
    private var sharedPlateNumber: String? = null
    private var sharedBusinessName: String? = null
    private var sharedLocationName: String? = null
    private var sharedProfileImage: String? = null
    private var sharedVehicleId: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var noDeliveriesText: View
    private val deliveries = mutableListOf<DeliveryRequest>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    abstract fun filterStatus(
        isStarted: Boolean?,
        isDone: Boolean?,
        status: String?,
        isDeclined: Boolean?,
        isAccepted: Boolean?
    ): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        sharedVehicleType = args?.getString("vehicleType")
        sharedVehicleModel = args?.getString("vehicleModel")
        sharedPlateNumber = args?.getString("plateNumber")
        sharedBusinessName = args?.getString("businessName")
        sharedLocationName = args?.getString("locationName")
        sharedProfileImage = args?.getString("profileImageUrl")
        sharedVehicleId= args?.getString("vehicleId")

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.deliveriesRecyclerView)
        noDeliveriesText = view.findViewById(R.id.noDeliveriesText)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = DeliveriesAdapter(deliveries) { delivery ->
            val intent = Intent(requireContext(), FarmerDeliveryDetailsActivity::class.java)
            intent.putExtra("requestId", delivery.requestId)
            intent.putExtra("pickupName", delivery.pickupName)
            intent.putExtra("destinationName", delivery.destinationName)
            intent.putExtra("purpose", delivery.purpose)
            intent.putExtra("productType", delivery.productType)
            intent.putExtra("weight", delivery.weight)
            intent.putExtra("estimatedCost", delivery.estimatedCost)
            intent.putExtra("estimatedTime", delivery.estimatedTime)
            intent.putExtra("businessName", delivery.businessName)
            intent.putExtra("locationName", delivery.locationName)
            intent.putExtra("profileImageUrl", delivery.profileImageUrl)
            intent.putExtra("vehicleType", delivery.vehicleType)
            intent.putExtra("vehicleModel", delivery.vehicleModel)
            intent.putExtra("plateNumber", delivery.plateNumber)
            startActivity(intent)
        }
        loadFilteredDeliveries()
    }

    private fun loadFilteredDeliveries() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("deliveryRequests")
            .whereEqualTo("farmerId", userId)
            .get()
            .addOnSuccessListener { requestDocs ->
                if (requestDocs.isEmpty) {
                    updateUI()
                    return@addOnSuccessListener
                }

                val tempDeliveries = mutableListOf<DeliveryRequest>()
                var completed = 0
                val total = requestDocs.size()

                fun finalizeIfDone() {
                    completed++
                    if (completed == total) {
                        deliveries.clear()
                        deliveries.addAll(tempDeliveries)
                        updateUI()
                    }
                }

                forLoop@for (doc in requestDocs) {
                    val requestId = doc.getString("requestId")
                    if (requestId == null) {
                        finalizeIfDone()
                        continue@forLoop
                    }

                    db.collection("deliveries").whereEqualTo("requestId", requestId).limit(1).get()
                        .addOnSuccessListener { deliveryDocs ->
                            val statusDoc = deliveryDocs.documents.firstOrNull()
                            val isStarted = statusDoc?.getBoolean("isStarted")
                            val isDone = statusDoc?.getBoolean("isDone")
                            val isAccepted = doc.getBoolean("isAccepted")
                            val isDeclined = doc.getBoolean("isDeclined")
                            val status = doc.getString("status")
                            val schedTime = doc.getTimestamp("scheduledTime")

                            val delivery = doc.toObject(DeliveryRequest::class.java)?.copy(
                                isStarted = isStarted ?: false,
                                isDone = isDone ?: false,
                                isAccepted = isAccepted ?: false,
                                isDeclined = isDeclined ?: false,
                                scheduledTime = schedTime
                            )

                            if (delivery == null || !filterStatus(isStarted, isDone, status, isDeclined, isAccepted)) {
                                finalizeIfDone()
                                return@addOnSuccessListener
                            }

                            sharedVehicleId = delivery.vehicleId
                            Log.d("SharedVhicleId", "vehicleId: $sharedVehicleId")

                            if (!sharedVehicleId.isNullOrEmpty()) {
                                db.collection("vehicles").document(sharedVehicleId!!).get()
                                    .addOnSuccessListener { vehicleDoc ->
                                        val vehicleType = vehicleDoc.getString("vehicleType") ?: "Unknown"
                                        val vehicleModel = vehicleDoc.getString("model") ?: "Unknown"
                                        val plateNumber = vehicleDoc.getString("plateNumber") ?: "Unknown"

                                        val updatedDelivery = delivery.copy(
                                            vehicleType = vehicleType,
                                            vehicleModel = vehicleModel,
                                            plateNumber = plateNumber
                                        )

                                        fetchBusinessInfoAndComplete(updatedDelivery, tempDeliveries, ::finalizeIfDone)
                                    }
                                    .addOnFailureListener {
                                        fetchBusinessInfoAndComplete(delivery, tempDeliveries, ::finalizeIfDone)
                                    }
                            } else {
                                fetchBusinessInfoAndComplete(delivery, tempDeliveries, ::finalizeIfDone)
                            }
                        }
                        .addOnFailureListener {
                            finalizeIfDone()
                        }
                }
            }
    }

    private fun fetchBusinessInfoAndComplete(
        delivery: DeliveryRequest,
        tempDeliveries: MutableList<DeliveryRequest>,
        finalizeIfDone: () -> Unit
    ) {
        val businessId = delivery.businessId
        if (!businessId.isNullOrEmpty()) {
            db.collection("users").document(businessId).get()
                .addOnSuccessListener { businessDoc ->

                    val updatedDelivery = delivery.copy(
                        businessName = businessDoc.getString("businessName") ?: sharedBusinessName ?: "Unknown",
                        profileImageUrl = businessDoc.getString("profileImageUrl") ?: sharedProfileImage ?: "",
                        locationName = businessDoc.getString("locationName") ?: sharedLocationName ?: "",
                    )
                    tempDeliveries.add(updatedDelivery)
                    finalizeIfDone()
                }
                .addOnFailureListener {
                    tempDeliveries.add(delivery)
                    finalizeIfDone()
                }
        } else {
            tempDeliveries.add(delivery)
            finalizeIfDone()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUI() {
        recyclerView.adapter?.notifyDataSetChanged()
        noDeliveriesText.visibility = if (deliveries.isEmpty()) View.VISIBLE else View.GONE
    }
}
