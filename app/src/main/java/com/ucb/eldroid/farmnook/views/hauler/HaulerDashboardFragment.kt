package com.ucb.eldroid.farmnook.views.hauler

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.*
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.DeliveryItem
import com.ucb.eldroid.farmnook.views.adapter.DeliveryAdapter

class HaulerDashboardFragment : Fragment() {

    private lateinit var deliveryAdapter: DeliveryAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        recyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        firestore = FirebaseFirestore.getInstance()

        fetchDeliveries()

        return rootView
    }

    private fun fetchDeliveries() {
        firestore.collection("deliveries")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                val deliveryList = snapshots?.documents?.mapNotNull { it.toObject(DeliveryItem::class.java) } ?: emptyList()

                deliveryAdapter = DeliveryAdapter(deliveryList)
                recyclerView.adapter = deliveryAdapter
            }
    }
}
