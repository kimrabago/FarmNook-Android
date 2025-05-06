package com.ucb.capstone.farmnook.ui.hauler


import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
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

        val transRecycler = rootView.findViewById<RecyclerView>(R.id.transRecyclerView)
        transRecycler.layoutManager = LinearLayoutManager(requireContext())

        val transactionList = mutableListOf<DeliveryHistory>()
        val farmerNameMap = mutableMapOf<String, String>() // deliveryId to farmerName
        val farmerImageMap = mutableMapOf<String, String?>()

        val now = Calendar.getInstance()

        historyListener = firestore.collection("deliveryHistory")
            .addSnapshotListener { historyDocs, error ->
                if (error != null || historyDocs == null) return@addSnapshotListener

                transactionList.clear()
                farmerNameMap.clear()
                farmerImageMap.clear()

                for (doc in historyDocs) {
                    val history = doc.toObject(DeliveryHistory::class.java)
                    val deliveryId = history.deliveryId
                    val arrivalDate = history.deliveryArrivalTime?.toDate() ?: continue

                    val cal = Calendar.getInstance().apply { time = arrivalDate }

                    firestore.collection("deliveries").document(deliveryId)
                        .addSnapshotListener { deliveryDoc, _ ->
                            if (deliveryDoc == null || !deliveryDoc.exists()) return@addSnapshotListener

                            val requestId = deliveryDoc.getString("requestId") ?: return@addSnapshotListener

                            firestore.collection("deliveryRequests").document(requestId)
                                .addSnapshotListener { reqDoc, _ ->
                                    if (reqDoc == null || !reqDoc.exists()) return@addSnapshotListener

                                    val farmerId = reqDoc.getString("farmerId") ?: return@addSnapshotListener

                                    firestore.collection("users").document(farmerId)
                                        .addSnapshotListener { farmerDoc, _ ->
                                            if (farmerDoc == null || !farmerDoc.exists()) return@addSnapshotListener

                                            val name = "${farmerDoc.getString("firstName") ?: ""} ${farmerDoc.getString("lastName") ?: ""}"
                                            farmerNameMap[deliveryId] = name
                                            val imageUrl = farmerDoc.getString("profileImageUrl")
                                            farmerImageMap[deliveryId] = imageUrl

                                            val pickup = reqDoc.getString("pickupLocation") ?: "Unknown"
                                            val drop = reqDoc.getString("destinationLocation") ?: "Unknown"
                                            val pickupAddress = reqDoc.getString("pickupName") ?: "Unknown"
                                            val destinationAddress = reqDoc.getString("destinationName") ?: "Unknown"
                                            val estimatedTime = reqDoc.getString("estimatedTime") ?: "Unknown"

                                            if (cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                                                if (!transactionList.contains(history)) {
                                                    transactionList.add(history)
                                                }
                                                transRecycler.adapter = DeliveryHistoryAdapter(
                                                    transactionList,
                                                    farmerNameMap,
                                                    farmerImageMap
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

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyListener?.remove()
    }
}