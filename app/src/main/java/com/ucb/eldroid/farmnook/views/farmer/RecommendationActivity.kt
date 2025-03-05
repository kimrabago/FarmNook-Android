package com.ucb.eldroid.farmnook.views.farmer

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.Hauler
import com.ucb.eldroid.farmnook.views.adapter.HaulerAdapter

class RecommendationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        // Retrieve selected data
        val vehicleType = intent.getStringExtra("vehicleType")
        val productType = intent.getStringExtra("productType")
        val weight = intent.getStringExtra("weight")

        // Display selection info
        val recommendationTextView: TextView = findViewById(R.id.recommendation_description)
        recommendationTextView.text = "Recommended haulers for: $vehicleType, $productType, $weight"

        // Set up RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recommended_haulers_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Sample haulers data
        val haulerList = listOf(
            Hauler("John Doe", "4.8 ⭐", "₱2500", "ABC123", "Cebu City", "Truck", "Isuzu F-Series", "5000kg", true),
            Hauler("Jane Smith", "4.5 ⭐", "₱2250", "XYZ789", "Mandaue City", "Van", "Toyota HiAce", "1000kg", false),
            Hauler("Michael Lee", "4.9 ⭐", "₱2750", "LMN456", "Lapu-Lapu City", "Truck", "Mitsubishi Fuso", "4500kg", true)
        )

        // Set adapter
        val adapter = HaulerAdapter(this, haulerList)
        recyclerView.adapter = adapter

        // Cancel button - return to previous activity
        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }
    }
}
