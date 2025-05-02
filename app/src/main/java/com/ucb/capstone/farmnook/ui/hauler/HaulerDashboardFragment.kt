package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.*
import android.view.*
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryDisplayItem
import com.ucb.capstone.farmnook.ui.adapter.AssignedDeliveryAdapter
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import com.ucb.capstone.farmnook.utils.EstimateTravelTimeUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

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

        val tempList = mutableListOf<DeliveryDisplayItem>()

        val recyclerView: RecyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        assignedDeliveryAdapter = AssignedDeliveryAdapter(deliveryList) { delivery ->
            val intent = Intent(requireContext(), DeliveryDetailsActivity::class.java).apply {
                putExtra("deliveryId", delivery.deliveryId)
                putExtra("pickupAddress", delivery.pickupLocation)
                putExtra("destinationAddress", delivery.destinationLocation)
                putExtra("pickup", delivery.rawPickup)
                putExtra("destination", delivery.rawDrop)
                putExtra("estimatedTime", delivery.estimatedTime)
                putExtra("totalCost", delivery.totalCost)
                putExtra("requestId", delivery.requestId)
            }
            startActivity(intent)
        }
        recyclerView.adapter = assignedDeliveryAdapter

        profileImageFetch()
        loadDeliveries()

        return rootView
    }

    private fun profileImageFetch() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get(Source.CACHE)
                .addOnSuccessListener { document ->
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@HaulerDashboardFragment)
                            .load(imageUrl)
                            .placeholder(R.drawable.profile_circle)
                            .error(R.drawable.profile_circle)
                            .into(profileIcon)
                    } else {
                        profileIcon.setImageResource(R.drawable.profile_circle)
                    }
                }
                .addOnFailureListener {
                    profileIcon.setImageResource(R.drawable.profile_circle)
                }
        }
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

                            val requestDoc = firestore.collection("deliveryRequests").document(requestId).get().await()
                            val totalCost = requestDoc.getLong("estimatedCost")?.toInt()?.toString() ?: return@async null
                            val estimatedTime = requestDoc.getString("estimatedTime") ?: return@async null
                            val pickup = requestDoc.getString("pickupLocation") ?: return@async null
                            val drop = requestDoc.getString("destinationLocation") ?: return@async null

                            val pickupAddressDeferred = async { reverseGeocode(pickup) }
                            val dropAddressDeferred = async { reverseGeocode(drop) }

                            val pickupAddress = pickupAddressDeferred.await()
                            val dropAddress = dropAddressDeferred.await()

                            DeliveryDisplayItem(deliveryId, pickupAddress, dropAddress, pickup, drop, estimatedTime, totalCost, requestId)
                        }
                    }

                    deliveryList.addAll(jobs.mapNotNull { it.await() })
                    assignedDeliveryAdapter.notifyDataSetChanged()
                }
            }
    }

    suspend fun reverseGeocode(latLng: String): String = suspendCancellableCoroutine { cont ->
        val (lat, lng) = latLng.split(",").map { it.trim().toDouble() }
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=$mapboxToken"

        val request = Request.Builder().url(url).build()
        val call = OkHttpClient().newCall(request)

        cont.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume("Unknown", null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val address = try {
                    val json = JSONObject(body ?: "")
                    json.getJSONArray("features")
                        .optJSONObject(0)
                        ?.getString("place_name") ?: "Unknown location"
                } catch (e: Exception) {
                    "Unknown location"
                }

                if (cont.isActive) cont.resume(address, null)
            }
        })
    }
}
