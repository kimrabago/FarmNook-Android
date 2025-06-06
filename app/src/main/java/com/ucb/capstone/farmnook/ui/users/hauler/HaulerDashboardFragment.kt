package com.ucb.capstone.farmnook.ui.users.hauler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryDisplayItem
import com.ucb.capstone.farmnook.ui.adapter.AssignedDeliveryAdapter
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class HaulerDashboardFragment : Fragment() {

    private lateinit var assignedDeliveryAdapter: AssignedDeliveryAdapter
    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val deliveryList = mutableListOf<DeliveryDisplayItem>()
    private val mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profileImage)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)

        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        profileIcon.setOnClickListener {
            (activity as? NavigationBar)?.navigateToProfile()
        }

        val recyclerView: RecyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        assignedDeliveryAdapter = AssignedDeliveryAdapter(deliveryList, false) { delivery ->

            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val scheduleTimeStr = delivery.scheduledTime?.toDate()?.let { formatter.format(it) } ?: ""
            Log.d("INTENT_DEBUG", "Putting scheduleTime: $scheduleTimeStr")

            val intent = Intent(requireContext(), DeliveryDetailsActivity::class.java).apply {
                putExtra("deliveryId", delivery.deliveryId)
                putExtra("pickupAddress", delivery.pickupLocation)
                putExtra("destinationAddress", delivery.destinationLocation)
                putExtra("pickup", delivery.rawPickup)
                putExtra("destination", delivery.rawDrop)
                putExtra("estimatedTime", delivery.estimatedTime)
                putExtra("totalCost", delivery.totalCost)
                putExtra("requestId", delivery.requestId)
                putExtra("requestId", delivery.requestId)
                putExtra("receiverName", delivery.receiverName)
                putExtra("receiverNum", delivery.receiverNum)
                putExtra("deliveryNote", delivery.deliveryNote)
                putExtra("scheduleTime", scheduleTimeStr)
            }
            startActivity(intent)
        }
        recyclerView.adapter = assignedDeliveryAdapter

        loadDeliveries()

        return rootView
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadDeliveries() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("deliveries")
            .whereEqualTo("haulerAssignedId", userId)
            .addSnapshotListener { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener

                lifecycleScope.launch {
                    deliveryList.clear()

                    val jobs = docs.map { doc ->
                        async {
                            val deliveryId = doc.id
                            val requestId = doc.getString("requestId") ?: return@async null
                            val isStarted = doc.getBoolean("isStarted") ?: false
                            val isDone = doc.getBoolean("isDone") ?: false
                            val scheduledTime = doc.getTimestamp("scheduledTime") ?: return@async null
                            if (isDone) return@async null

                            val requestDoc = firestore.collection("deliveryRequests").document(requestId).get().await()
                            val totalCost = requestDoc.getLong("estimatedCost")?.toInt()?.toString() ?: return@async null
                            val estimatedTime = requestDoc.getString("estimatedTime") ?: return@async null
                            val pickup = requestDoc.getString("pickupLocation") ?: return@async null
                            val drop = requestDoc.getString("destinationLocation") ?: return@async null
                            val pickupAddress =  requestDoc.getString("pickupName") ?: return@async null
                            val dropAddress = requestDoc.getString("destinationName") ?: return@async null
                            val receiverName = requestDoc.getString("receiverName") ?: ""
                            val receiverNum = requestDoc.getString("receiverNumber") ?: ""
                            val deliveryNote = requestDoc.getString("deliveryNote") ?: ""

                            DeliveryDisplayItem(deliveryId, pickupAddress, dropAddress, pickup, drop, estimatedTime, totalCost, requestId, receiverName, receiverNum, deliveryNote, isStarted, scheduledTime)
                        }
                    }

                    val resultList = jobs.mapNotNull { it.await() }
                    val anyStarted = resultList.any { it.isStarted }

                    deliveryList.addAll(resultList)
                    assignedDeliveryAdapter.updateData(deliveryList, anyStarted)
                }
            }
    }
}
