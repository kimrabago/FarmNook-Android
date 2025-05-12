package com.ucb.capstone.farmnook.ui.users.farmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import com.ucb.capstone.farmnook.utils.loadImage
import de.hdodenhof.circleimageview.CircleImageView

class FarmerDeliveryStatusFragment : Fragment(R.layout.fragment_farmer_delivery_status) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noDeliveriesText: View
    private val deliveries = mutableListOf<DeliveryRequest>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isLoading = false
    private var currentLoadJob: kotlinx.coroutines.Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            initializeViews(view)
            setupRecyclerView()
            loadDeliveries()
        } catch (e: Exception) {
            Log.e("FarmerDeliveryStatus", "Error in onViewCreated: ${e.message}")
            showError("Failed to initialize delivery status view")
        }
    }

    private fun initializeViews(view: View) {
        try {
            recyclerView = view.findViewById(R.id.deliveriesRecyclerView)
            noDeliveriesText = view.findViewById(R.id.noDeliveriesText)
        } catch (e: Exception) {
            Log.e("FarmerDeliveryStatus", "Error initializing views: ${e.message}")
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = DeliveriesAdapter(deliveries) { delivery ->
                try {
                    launchDeliveryDetails(delivery)
                } catch (e: Exception) {
                    Log.e("FarmerDeliveryStatus", "Error launching delivery details: ${e.message}")
                    showError("Failed to open delivery details")
                }
            }
        } catch (e: Exception) {
            Log.e("FarmerDeliveryStatus", "Error setting up RecyclerView: ${e.message}")
            throw e
        }
    }

    private fun launchDeliveryDetails(delivery: DeliveryRequest) {
        Intent(requireContext(), FarmerDeliveryDetailsActivity::class.java).apply {
            putExtra("requestId", delivery.requestId)
            putExtra("pickupName", delivery.pickupName)
            putExtra("destinationName", delivery.destinationName)
            putExtra("purpose", delivery.purpose)
            putExtra("productType", delivery.productType)
            putExtra("weight", delivery.weight)
            putExtra("estimatedCost", delivery.estimatedCost)
            putExtra("estimatedTime", delivery.estimatedTime)
            putExtra("businessName", delivery.businessName)
            putExtra("locationName", delivery.locationName)
            putExtra("profileImageUrl", delivery.profileImageUrl)
            putExtra("vehicleType", delivery.vehicleType)
            putExtra("vehicleModel", delivery.vehicleModel)
            putExtra("plateNumber", delivery.plateNumber)
        }.also { startActivity(it) }
    }

    private fun loadDeliveries() {
        if (isLoading) return
        isLoading = true

        val userId = auth.currentUser?.uid ?: run {
            showError("User not authenticated")
            isLoading = false
            return
        }

        // Clear existing data
        deliveries.clear()
        updateUI()

        db.collection("deliveryRequests")
            .whereEqualTo("farmerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    isLoading = false
                    updateUI()
                    return@addOnSuccessListener
                }

                val pendingQueries = documents.size()
                var completedQueries = 0
                val tempDeliveries = mutableListOf<DeliveryRequest>()

                for (doc in documents) {
                    val requestId = doc.getString("requestId") ?: continue

                    db.collection("deliveries")
                        .whereEqualTo("requestId", requestId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { deliveryDocs ->
                            try {
                                val statusDoc = deliveryDocs.documents.firstOrNull()
                                val isDone = statusDoc?.getBoolean("isDone") ?: false
                                val isStarted = statusDoc?.getBoolean("isStarted") ?: false

                                val delivery = DeliveryRequest(
                                    id = doc.id,
                                    requestId = requestId,
                                    pickupName = doc.getString("pickupName"),
                                    destinationName = doc.getString("destinationName"),
                                    purpose = doc.getString("purpose"),
                                    productType = doc.getString("productType"),
                                    weight = doc.getString("weight"),
                                    estimatedCost = doc.getDouble("estimatedCost"),
                                    estimatedTime = doc.getString("estimatedTime"),
                                    businessName = doc.getString("businessName"),
                                    locationName = doc.getString("locationName"),
                                    profileImageUrl = doc.getString("profileImageUrl"),
                                    vehicleType = doc.getString("vehicleType"),
                                    vehicleModel = doc.getString("vehicleModel"),
                                    plateNumber = doc.getString("plateNumber"),
                                    isDone = isDone,
                                    isStarted = isStarted
                                )
                                tempDeliveries.add(delivery)
                            } catch (e: Exception) {
                                Log.e("FarmerDeliveryStatus", "Error parsing delivery document: ${e.message}")
                            }

                            completedQueries++
                            if (completedQueries == pendingQueries) {
                                // All queries are complete, update UI once
                                deliveries.clear()
                                deliveries.addAll(tempDeliveries)
                                isLoading = false
                                updateUI()
                            }
                        }
                        .addOnFailureListener {
                            Log.e("FarmerDeliveryStatus", "Failed to get delivery status for $requestId")
                            completedQueries++
                            if (completedQueries == pendingQueries) {
                                deliveries.clear()
                                deliveries.addAll(tempDeliveries)
                                isLoading = false
                                updateUI()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("FarmerDeliveryStatus", "Failed to load deliveries: ${it.message}")
                showError("Failed to load deliveries")
                isLoading = false
                updateUI()
            }
    }

    private fun updateUI() {
        if (!isAdded) return // Check if fragment is still attached to activity
        
        try {
            if (deliveries.isEmpty()) {
                noDeliveriesText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noDeliveriesText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            Log.e("FarmerDeliveryStatus", "Error updating UI: ${e.message}")
            showError("Failed to update delivery list")
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVisible && !isLoading) {
            loadDeliveries()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deliveries.clear()
        isLoading = false
        currentLoadJob?.cancel()
    }

    private fun showError(message: String) {
        if (!isAdded) return
        try {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("FarmerDeliveryStatus", "Error showing error message: ${e.message}")
        }
    }
}

class DeliveriesAdapter(
    private val deliveries: List<DeliveryRequest>,
    private val onItemClick: (DeliveryRequest) -> Unit
) : RecyclerView.Adapter<DeliveriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deliveryId: TextView = view.findViewById(R.id.deliveryIdText)
        val deliveryStatus: TextView = view.findViewById(R.id.deliveryStatusText)
        val haulerName: TextView = view.findViewById(R.id.haulerNameText)
        val vehicleInfo: TextView = view.findViewById(R.id.vehicleInfoText)
        val pickupLocation: TextView = view.findViewById(R.id.pickupLocationText)
        val destinationLocation: TextView = view.findViewById(R.id.destinationLocationText)
        val messageButton: ImageButton = view.findViewById(R.id.messageButton)
        val viewSummaryButton: Button = view.findViewById(R.id.viewSummaryButton)
        val haulerProfileImage: CircleImageView = view.findViewById(R.id.haulerProfileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_delivery_status, parent, false)
            ViewHolder(view)
        } catch (e: Exception) {
            Log.e("DeliveriesAdapter", "Error creating ViewHolder: ${e.message}")
            throw e
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val delivery = deliveries[position]

            holder.deliveryId.text = "Delivery #${delivery.requestId?.take(8)}"
            holder.pickupLocation.text = "From: ${delivery.pickupName}"
            holder.destinationLocation.text = "To: ${delivery.destinationName}"
            holder.vehicleInfo.text = "${delivery.vehicleType ?: "N/A"} - ${delivery.vehicleModel ?: "N/A"}"

            // Add null check for profile image
            if (!delivery.profileImageUrl.isNullOrEmpty()) {
                holder.haulerProfileImage.loadImage(delivery.profileImageUrl)
            } else {
                holder.haulerProfileImage.setImageResource(R.drawable.profile_circle) // Set default image
            }

            holder.viewSummaryButton.setOnClickListener { onItemClick(delivery) }
            holder.messageButton.setOnClickListener {
                try {
                    handleMessageButtonClick(delivery, holder.itemView.context)
                } catch (e: Exception) {
                    Log.e("DeliveriesAdapter", "Error handling message button click: ${e.message}")
                    Toast.makeText(holder.itemView.context, "Failed to open chat", Toast.LENGTH_SHORT).show()
                }
            }

            // Update status based on delivery state
            holder.deliveryStatus.text = when {
                delivery.isDone == true -> "Completed"
                delivery.isStarted == true -> "In Progress"
                else -> "Pending"
            }
        } catch (e: Exception) {
            Log.e("DeliveriesAdapter", "Error binding ViewHolder: ${e.message}")
        }
    }

    private fun handleMessageButtonClick(delivery: DeliveryRequest, context: android.content.Context) {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance().collection("deliveries")
            .whereEqualTo("requestId", delivery.requestId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(context, "No delivery found", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    
                    val deliveryDoc = querySnapshot.documents[0]
                    val haulerId = deliveryDoc.getString("haulerAssignedId") ?: run {
                        Toast.makeText(context, "No hauler assigned", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val chatId = if (farmerId < haulerId) "$farmerId-$haulerId"
                    else "$haulerId-$farmerId"

                    FirebaseFirestore.getInstance().collection("users").document(haulerId).get()
                        .addOnSuccessListener { haulerDoc ->
                            try {
                                val firstName = haulerDoc.getString("firstName") ?: ""
                                val lastName = haulerDoc.getString("lastName") ?: ""
                                val haulerName = "$firstName $lastName".trim()

                                Intent(context, MessageActivity::class.java).apply {
                                    putExtra("chatId", chatId)
                                    putExtra("recipientId", haulerId)
                                    putExtra("receiverName", haulerName)
                                }.also { context.startActivity(it) }
                            } catch (e: Exception) {
                                Log.e("DeliveriesAdapter", "Error launching message activity: ${e.message}")
                                Toast.makeText(context, "Failed to open chat", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeliveriesAdapter", "Error fetching hauler details: ${e.message}")
                            Toast.makeText(context, "Failed to load hauler details", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    Log.e("DeliveriesAdapter", "Error processing delivery document: ${e.message}")
                    Toast.makeText(context, "Failed to process delivery details", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeliveriesAdapter", "Error fetching delivery: ${e.message}")
                Toast.makeText(context, "Failed to load delivery details", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount() = deliveries.size
}