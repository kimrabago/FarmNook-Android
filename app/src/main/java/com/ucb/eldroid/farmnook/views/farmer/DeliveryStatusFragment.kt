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
import com.ucb.eldroid.farmnook.R

class DeliveryStatusFragment : Fragment(R.layout.fragment_delivery_status) {

    private lateinit var menuBurger: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var deliveryInfoLayout: View
    private lateinit var gestureDetector: GestureDetector
    private var isHidden = false

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
        // Initialize GestureDetector
        gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null || e2 == null) return false

                return when {
                    e1.y - e2.y > 100 -> {
                        // Swipe Up - Hide the delivery info
                        if (!isHidden) {
                            slideUp(deliveryInfoLayout)
                            isHidden = true
                        }
                        true
                    }
                    e2.y - e1.y > 100 -> {
                        // Swipe Down - Show the delivery info
                        if (isHidden) {
                            slideDown(deliveryInfoLayout)
                            isHidden = false
                        }
                        true
                    }
                    else -> false
                }
            }
        })

        // Set touch listener to detect gestures
        view.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun slideUp(view: View) {
        view.animate().translationY(view.height.toFloat()).setDuration(300)
    }

    private fun slideDown(view: View) {
        view.animate().translationY(0f).setDuration(300)
    }
}
