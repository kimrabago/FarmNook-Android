package com.ucb.eldroid.farmnook.views.hauler

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.DeliveryItem
import com.ucb.eldroid.farmnook.views.adapter.DeliveriesAdapter
import com.ucb.eldroid.farmnook.views.auth.LoginActivity
import com.ucb.eldroid.farmnook.views.hauler.subscription.SubscriptionActivity
import com.ucb.eldroid.farmnook.views.settings.*

class HaulerDashboardFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_hauler_dashboard, container, false)

        // Initialize views
        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profile_icon)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        navigationView = requireActivity().findViewById(R.id.navigation_view)

        // Open navigation drawer when clicking menu icon
        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Navigate to Profile when clicking the profile icon
        profileIcon.setOnClickListener {
            startActivity(Intent(requireContext(), HaulerProfileActivity::class.java))
        }

        // Handle navigation menu clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile -> startActivity(Intent(requireContext(), HaulerProfileActivity::class.java))
                R.id.notification -> startActivity(Intent(requireContext(), NotificationActivity::class.java))
                R.id.about -> startActivity(Intent(requireContext(), AboutActivity::class.java))
                R.id.subscription -> startActivity(Intent(requireContext(), SubscriptionActivity::class.java))
                R.id.report -> startActivity(Intent(requireContext(), ReportActivity::class.java))
                R.id.feedback -> startActivity(Intent(requireContext(), FeedbackActivity::class.java))
                R.id.nav_logout -> {
                    // Handle logout logic here (e.g., clear session, redirect to login screen)
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after selection
            true
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
        recyclerView.adapter = DeliveriesAdapter(deliveryList)

        return rootView
    }
}
