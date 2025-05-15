// FarmerDeliveryStatusFragment.kt (Updated to use tabs with ViewPager2)

package com.ucb.capstone.farmnook.ui.users.farmer

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.adapter.DeliveryStatusPagerAdapter
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.CancelledDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.CompletedDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.DeclinedDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.InProgressDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.PendingDeliveriesFragment

class FarmerDeliveryStatusFragment : Fragment(R.layout.fragment_farmer_delivery_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.statusTabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.statusViewPager)

        val args = arguments
        Log.d("BaseDeliveryFragment", "vehicleType: ${args?.getString("vehicleType")}")
        val detailsBundle = Bundle().apply {
            putString("requestId", args?.getString("requestId"))
            putString("pickupName", args?.getString("pickupName"))
            putString("destinationName", args?.getString("destinationName"))
            putString("purpose", args?.getString("purpose"))
            putString("productType", args?.getString("productType"))
            putString("weight", args?.getString("weight"))
            putDouble("estimatedCost", args?.getDouble("estimatedCost") ?: 0.0)
            putString("estimatedTime", args?.getString("estimatedTime"))
            putString("businessName", args?.getString("businessName"))
            putString("locationName", args?.getString("locationName"))
            putString("profileImageUrl", args?.getString("profileImageUrl"))
            putString("vehicleId", args?.getString("vehicleId"))
            putString("vehicleType", args?.getString("vehicleType"))
            putString("vehicleModel", args?.getString("vehicleModel"))
            putString("plateNumber", args?.getString("plateNumber"))
        }

        val adapter = DeliveryStatusPagerAdapter(requireActivity(), detailsBundle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pending"
                1 -> "In Progress"
                2 -> "Completed"
                3 -> "Declined"
                4 -> "Cancelled"
                else -> ""
            }
        }.attach()
    }
}


