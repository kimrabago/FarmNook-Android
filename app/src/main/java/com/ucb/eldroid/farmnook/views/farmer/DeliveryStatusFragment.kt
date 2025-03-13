package com.ucb.eldroid.farmnook.views.farmer

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ucb.eldroid.farmnook.R

class DeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var menuBurger: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var deliveryInfoLayout: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuBurger = view.findViewById(R.id.menu_burger)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        deliveryInfoLayout = view.findViewById(R.id.deliveryInfoLayout)


        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false
    }
}
