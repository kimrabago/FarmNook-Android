package com.ucb.capstone.farmnook.ui.hauler

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*

import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.adapter.AssignedDeliveryAdapter
import com.ucb.capstone.farmnook.data.model.DeliveryDisplayItem
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HaulerDashboardFragment : Fragment() {

    private lateinit var AssignedDeliveryAdapter: AssignedDeliveryAdapter
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
        profileIcon = rootView.findViewById(R.id.profile_icon)
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
        AssignedDeliveryAdapter = AssignedDeliveryAdapter(deliveryList)
        recyclerView.adapter = AssignedDeliveryAdapter

        loadDeliveries()
        return rootView
    }

    private fun loadDeliveries() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("deliveries")
            .whereEqualTo("haulerAssignedId", userId)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val deliveryId = doc.id
                    val requestId = doc.getString("requestId") ?: continue
                    val dateJoined = doc.getTimestamp("dateJoined")?.toDate()
                    val formattedTime = dateJoined?.let {
                        SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(it)
                    } ?: "Unknown"

                    firestore.collection("deliveryRequests").document(requestId)
                        .get()
                        .addOnSuccessListener { req ->
                            val pickup =
                                req.getString("pickupLocation") ?: return@addOnSuccessListener
                            val drop =
                                req.getString("destinationLocation") ?: return@addOnSuccessListener

                            getEstimatedTravelTime(pickup, drop) { estimatedTime ->
                                reverseGeocode(pickup) { pickupAddress ->
                                    reverseGeocode(drop) { dropAddress ->
                                        val item = DeliveryDisplayItem(
                                            deliveryId = deliveryId,
                                            pickupLocation = pickupAddress,
                                            destinationLocation = dropAddress,
                                            estimatedTime = estimatedTime
                                        )
                                        deliveryList.add(item)
                                        AssignedDeliveryAdapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                }
            }
    }

    private fun reverseGeocode(latLng: String, callback: (String) -> Unit) {
        val (lat, lng) = latLng.split(",").map { it.trim().toDouble() }
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=$mapboxToken"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Unknown location")
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
                    Handler(Looper.getMainLooper()).post { callback(address) }
                }
            })
    }
    private fun getEstimatedTravelTime(pickup: String, drop: String, callback: (String) -> Unit) {
        val (startLat, startLng) = pickup.split(",").map { it.trim() }
        val (endLat, endLng) = drop.split(",").map { it.trim() }

        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$startLng,$startLat;$endLng,$endLat" +
                "?access_token=$mapboxToken&overview=false"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Unknown")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val durationSec = routes.getJSONObject(0).getDouble("duration") // in seconds
                        val minutes = (durationSec / 60).toInt()
                        val estimated = if (minutes < 60) "$minutes min"
                        else "${minutes / 60} hr ${minutes % 60} min"
                        Handler(Looper.getMainLooper()).post { callback(estimated) }
                    } else {
                        callback("Unknown")
                    }
                } catch (e: Exception) {
                    callback("Unknown")
                }
            }
        })
    }
}