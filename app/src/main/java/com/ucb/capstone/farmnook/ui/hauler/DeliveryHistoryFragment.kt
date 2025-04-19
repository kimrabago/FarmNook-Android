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
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryHistory
import com.ucb.capstone.farmnook.ui.adapter.DeliveryHistoryAdapter
import java.text.SimpleDateFormat
import java.util.*


class DeliveryHistoryFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_delivery_history, container, false)

        val transRecycler = rootView.findViewById<RecyclerView>(R.id.transRecyclerView)
        transRecycler.layoutManager = LinearLayoutManager(requireContext())

        val transactionList = mutableListOf<DeliveryHistory>()
        val farmerNameMap = mutableMapOf<String, String>() // deliveryId to farmerName

        val now = Calendar.getInstance()

        firestore.collection("deliveryHistory").get().addOnSuccessListener { historyDocs ->
            for (doc in historyDocs) {
                val history = doc.toObject(DeliveryHistory::class.java)
                val deliveryId = history.deliveryId
                val arrivalDate = history.deliveryArrivalTime?.toDate() ?: continue

                val cal = Calendar.getInstance().apply { time = arrivalDate }

                firestore.collection("deliveries").document(deliveryId).get()
                    .addOnSuccessListener { deliveryDoc ->
                        val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                        firestore.collection("deliveryRequests").document(requestId).get()
                            .addOnSuccessListener { reqDoc ->
                                val farmerId = reqDoc.getString("farmerId") ?: return@addOnSuccessListener
                                firestore.collection("users").document(farmerId).get()
                                    .addOnSuccessListener { farmerDoc ->
                                        val name = "${farmerDoc.getString("firstName") ?: ""} ${farmerDoc.getString("lastName") ?: ""}"
                                        farmerNameMap[deliveryId] = name

                                        if (cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                                            transactionList.add(history)
                                            transRecycler.adapter = DeliveryHistoryAdapter(
                                                transactionList,
                                                farmerNameMap
                                            ) { selectedDeliveryId ->
                                                val intent = Intent(requireContext(), HistoryDetailsActivity::class.java)
                                                intent.putExtra("deliveryId", selectedDeliveryId)
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
    }