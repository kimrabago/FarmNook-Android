package com.ucb.eldroid.farmnook.ui.hauler

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.data.model.DeliveryItem
import com.ucb.eldroid.farmnook.ui.adapter.DeliveryAdapter
import com.ucb.eldroid.farmnook.ui.menu.BottomNavigationBar

class HaulerDashboardFragment : Fragment() {

    private lateinit var deliveryAdapter: DeliveryAdapter
    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewDeliverBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        // Initialize views
        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profile_icon)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout) // Get Drawer from Activity

        // Open navigation drawer when clicking menu icon
        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Navigate to Profile when clicking the profile icon
        profileIcon.setOnClickListener {
            // Call global navigation handler in activity
            (activity as? BottomNavigationBar)?.navigateToProfile()
        }

        // Sample deliveries data
        val deliveryList = listOf(
            DeliveryItem(1, "Manila", "Metro Manila", "Quezon City", "Metro Manila", "30 mins", "₱150", "path/to/profile_image_1.jpg"),
            DeliveryItem(2, "Cebu", "Cebu", "Davao", "Davao", "2 hours", "₱1000", "path/to/profile_image_2.jpg")
        )

        // Set up RecyclerView
        val recyclerView: RecyclerView = rootView.findViewById(R.id.deliveries_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = DeliveryAdapter(deliveryList)

        return rootView
    }
}