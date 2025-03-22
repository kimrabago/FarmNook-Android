package com.ucb.capstone.farmnook.ui.farmer

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R

class HaulerDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hauler_dialog)

        // Get data from intent
        val name = intent.getStringExtra("name")
        val rating = intent.getStringExtra("rating")
        val price = intent.getStringExtra("price")
        val plateNumber = intent.getStringExtra("plateNumber")
        val location = intent.getStringExtra("location")
        val vehicleType = intent.getStringExtra("vehicleType")
        val model = intent.getStringExtra("model")
        val capacity = intent.getStringExtra("capacity")

        // Set data to UI elements
        findViewById<TextView>(R.id.haulerName).text = name
        findViewById<TextView>(R.id.plateNumber).text = plateNumber
        findViewById<TextView>(R.id.location).text = location
        findViewById<TextView>(R.id.vehicleType).text = vehicleType
        findViewById<TextView>(R.id.model).text = model
        findViewById<TextView>(R.id.capacity).text = capacity

        // Close button
        findViewById<ImageView>(R.id.closeDialog).setOnClickListener {
            finish()
        }

        // Hire Now button (Placeholder action)
        findViewById<Button>(R.id.hireNowButton).setOnClickListener {
            // Implement hiring functionality here
        }

        // Set Schedule button (Placeholder action)
        findViewById<Button>(R.id.setScheduleButton).setOnClickListener {
            // Implement scheduling functionality here
        }
    }
}
