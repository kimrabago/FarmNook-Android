package com.ucb.capstone.farmnook.ui.users


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.ui.adapter.DeliveryHistoryAdapter
import java.text.SimpleDateFormat
import java.util.*


class DeliveryHistoryFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var historyListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_delivery_history, container, false)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return rootView

        val transRecycler = rootView.findViewById<RecyclerView>(R.id.transRecyclerView)
        transRecycler.layoutManager = LinearLayoutManager(requireContext())

        val transactionList = mutableListOf<DeliveryHistory>()
        val nameMap = mutableMapOf<String, String>()
        val imageMap = mutableMapOf<String, String?>()

        val now = Calendar.getInstance()

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDoc ->
                val userType = userDoc.getString("userType") ?: "Farmer"

                //Load history
                historyListener = firestore.collection("deliveryHistory")
                    .addSnapshotListener { historyDocs, error ->
                        if (error != null || historyDocs == null) return@addSnapshotListener

                        transactionList.clear()
                        nameMap.clear()
                        imageMap.clear()

                        for (doc in historyDocs) {
                            val history = doc.toObject(DeliveryHistory::class.java)
                            val deliveryId = history.deliveryId
                            val arrivalDate = history.deliveryArrivalTime?.toDate() ?: continue

                            val cal = Calendar.getInstance().apply { time = arrivalDate }

                            firestore.collection("deliveries").document(deliveryId)
                                .get().addOnSuccessListener { deliveryDoc ->
                                    if (deliveryDoc == null || !deliveryDoc.exists()) return@addOnSuccessListener

                                    val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener

                                    val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                                    firestore.collection("deliveryRequests").document(requestId)
                                        .get().addOnSuccessListener { reqDoc ->
                                            if (reqDoc == null || !reqDoc.exists()) return@addOnSuccessListener

                                            val farmerId = reqDoc.getString("farmerId") ?: return@addOnSuccessListener
                                            val pickup = reqDoc.getString("pickupLocation") ?: "Unknown"
                                            val drop = reqDoc.getString("destinationLocation") ?: "Unknown"
                                            val pickupAddress = reqDoc.getString("pickupName") ?: "Unknown"
                                            val destinationAddress = reqDoc.getString("destinationName") ?: "Unknown"
                                            val estimatedTime = reqDoc.getString("estimatedTime") ?: "Unknown"

                                            // Only add if current user was involved
                                            val involved = (userType == "Farmer" && currentUserId == farmerId) ||
                                                    (userType != "Farmer" && currentUserId == haulerId)
                                            if (!involved) return@addOnSuccessListener

                                            val targetUserId = if (userType == "Farmer") haulerId else farmerId

                                            firestore.collection("users").document(targetUserId)
                                                .get().addOnSuccessListener { userDocSnap ->
                                                    if (userDocSnap == null || !userDocSnap.exists()) return@addOnSuccessListener

                                                    val name = "${userDocSnap.getString("firstName") ?: ""} ${userDocSnap.getString("lastName") ?: ""}"
                                                    val imageUrl = userDocSnap.getString("profileImageUrl")

                                                    nameMap[deliveryId] = name
                                                    imageMap[deliveryId] = imageUrl

                                                    if (cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                                                        if (!transactionList.contains(history)) {
                                                            transactionList.add(history)
                                                        }
                                                        transRecycler.adapter = DeliveryHistoryAdapter(
                                                            transactionList,
                                                            nameMap,
                                                            imageMap
                                                        ) { selectedDeliveryId ->
                                                            val intent = Intent(requireContext(), HistoryDetailsActivity::class.java)
                                                            intent.putExtra("deliveryId", selectedDeliveryId)
                                                            intent.putExtra("pickupAddress", pickupAddress)
                                                            intent.putExtra("destinationAddress", destinationAddress)
                                                            intent.putExtra("pickup", pickup)
                                                            intent.putExtra("destination", drop)
                                                            intent.putExtra("farmerName", name)
                                                            intent.putExtra("profileImg", imageUrl)
                                                            intent.putExtra("estimatedTime", estimatedTime)
                                                            startActivity(intent)
                                                        }
                                                    }
                                                }
                                        }
                                }
                        }
                    }
            }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyListener?.remove()
    }
}

