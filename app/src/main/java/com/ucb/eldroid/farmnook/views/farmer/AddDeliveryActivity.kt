package com.ucb.eldroid.farmnook.views.farmer

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R

class AddDeliveryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delivery)

        // Initialize Spinners
        val vehicleTypeSpinner: Spinner = findViewById(R.id.vehicle_type_spinner)
        val productTypeSpinner: Spinner = findViewById(R.id.product_type_spinner)
        val weightSpinner: Spinner = findViewById(R.id.weight_spinner)

        // Define selection options
        val vehicleTypes = arrayOf("Select Vehicle Type", "Small Truck", "Medium Truck", "Large Truck")
        val productTypes = arrayOf("Select Product Type", "Livestock", "Vegetables/Fruits", "Grains")
        val weights = arrayOf("Select Weight", "Less than 1 Ton", "1-5 Tons", "More than 5 Tons")

        // Create adapters
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)
        val productAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, productTypes)
        val weightAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, weights)

        // Set adapters to Spinners
        vehicleTypeSpinner.adapter = vehicleAdapter
        productTypeSpinner.adapter = productAdapter
        weightSpinner.adapter = weightAdapter

        // Search button action
        findViewById<Button>(R.id.search_button).setOnClickListener {
            val selectedVehicle = vehicleTypeSpinner.selectedItem.toString()
            val selectedProduct = productTypeSpinner.selectedItem.toString()
            val selectedWeight = weightSpinner.selectedItem.toString()

            if (selectedVehicle.contains("Select") || selectedProduct.contains("Select") || selectedWeight.contains("Select")) {
                Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show()
            } else {
                // Navigate to RecommendationActivity
                val intent = Intent(this, RecommendationActivity::class.java)
                intent.putExtra("vehicleType", selectedVehicle)
                intent.putExtra("productType", selectedProduct)
                intent.putExtra("weight", selectedWeight)
                startActivity(intent)
            }
        }

        // Cancel button action
        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish() // Closes activity
        }
    }
}
