package com.ucb.capstone.farmnook.ui.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.CancelledDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.CompletedDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.DeclinedDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.InProgressDeliveriesFragment
import com.ucb.capstone.farmnook.ui.users.farmer.deliveries.PendingDeliveriesFragment

class DeliveryStatusPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val sharedArgs: Bundle
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> PendingDeliveriesFragment()
            1 -> InProgressDeliveriesFragment()
            2 -> CompletedDeliveriesFragment()
            3 -> DeclinedDeliveriesFragment()
            4 -> CancelledDeliveriesFragment()
            else -> throw IllegalArgumentException("Invalid tab index")
        }
        fragment.arguments = Bundle(sharedArgs) // ✅ properly pass bundle to each tab
        return fragment
    }
}