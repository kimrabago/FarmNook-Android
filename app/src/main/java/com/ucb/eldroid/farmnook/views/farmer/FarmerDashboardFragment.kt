package com.ucb.eldroid.farmnook.views.farmer

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.menu.BottomNavigationBar

class FarmerDashboardFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var addDeliveryBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_farmer_dashboard, container, false)

        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profileImage)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout) // Get Drawer from Activity
        addDeliveryBtn = rootView.findViewById(R.id.addDeliveryBtn)

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

        addDeliveryBtn.setOnClickListener{
            val intent = Intent(requireContext(), AddDeliveryActivity::class.java)
            startActivity(intent)
        }
        return rootView
    }
}