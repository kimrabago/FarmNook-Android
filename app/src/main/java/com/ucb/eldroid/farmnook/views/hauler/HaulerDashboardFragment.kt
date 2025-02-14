package com.ucb.eldroid.farmnook.views.hauler

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


class HaulerDashboardFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        // Initialize the menu burger and drawer layout from rootView
        menuBurger = rootView.findViewById(R.id.menu_burger)
        drawerLayout = activity?.findViewById(R.id.drawer_layout) ?: return rootView

        // Set the click listener for the menu burger
        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

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

        val adapter = DeliveriesAdapter(deliveryList)
        val recyclerView: RecyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        return rootView
    }
}
