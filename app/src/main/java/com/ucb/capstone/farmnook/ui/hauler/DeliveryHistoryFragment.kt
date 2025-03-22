package com.ucb.capstone.farmnook.ui.hauler

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.ucb.capstone.farmnook.R


class DeliveryHistoryFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var drawerLayout: DrawerLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_delivery_history, container, false)

        menuBurger = rootView.findViewById(R.id.menu_burger)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout) // Get Drawer from Activity

        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        return rootView
    }

}