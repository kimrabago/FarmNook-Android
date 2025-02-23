package com.ucb.eldroid.farmnook.views.hauler

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.adapter.DeliveriesAdapter
import com.ucb.eldroid.farmnook.model.data.DeliveryItem
import com.ucb.eldroid.farmnook.views.settings.HaulerProfileActivity

class HaulerDashboardFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        // Initialize views
        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profile_icon) // Profile icon
        drawerLayout = activity?.findViewById(R.id.drawer_layout) ?: return rootView

        // Open navigation drawer
        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Navigate to HaulerProfileActivity when clicking profile icon
        profileIcon.setOnClickListener {
            val intent = Intent(requireContext(), HaulerProfileActivity::class.java)
            startActivity(intent)
        }

        // Sample deliveries data
        val deliveryList = listOf(
            DeliveryItem(
                id = 1,
                pickupLocation = "Manila",
                provincePickup = "Metro Manila",
                destination = "Quezon City",
                provinceDestination = "Metro Manila",
                estimatedTime = "30 mins",
                totalCost = "₱150",
                profileImage = "path/to/profile_image_1.jpg"
            ),
            DeliveryItem(
                id = 2,
                pickupLocation = "Cebu",
                provincePickup = "Cebu",
                destination = "Davao",
                provinceDestination = "Davao",
                estimatedTime = "2 hours",
                totalCost = "₱1000",
                profileImage = "path/to/profile_image_2.jpg"
            )
        )

        // Set up RecyclerView
        val adapter = DeliveriesAdapter(deliveryList)
        val recyclerView: RecyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        return rootView
    }
}
