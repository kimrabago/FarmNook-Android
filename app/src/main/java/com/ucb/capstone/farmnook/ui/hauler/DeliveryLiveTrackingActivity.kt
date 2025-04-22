package com.ucb.capstone.farmnook.ui.hauler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.hauler.fragments.DeliveryStatusFragment

class DeliveryLiveTrackingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_live_tracking)

        val fragment = DeliveryStatusFragment().apply {
            arguments = intent.extras
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
