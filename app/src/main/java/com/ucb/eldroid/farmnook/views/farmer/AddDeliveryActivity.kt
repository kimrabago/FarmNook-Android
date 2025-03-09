package com.ucb.eldroid.farmnook.views.farmer

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.DeliveryItem

class AddDeliveryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delivery)

        firestore = FirebaseFirestore.getInstance()

        val vehicleTypeSpinner: Spinner = findViewById(R.id.vehicle_type_spinner)
        val productTypeSpinner: Spinner = findViewById(R.id.product_type_spinner)
        val weightSpinner: Spinner = findViewById(R.id.weight_spinner)

        val vehicleTypes = arrayOf(
            "Select Vehicle Type",
            "Small Farm Truck",
            "Medium Farm Truck",
            "Large Farm Truck",
            "Tractor with Trailer",
            "Pickup Truck",
            "Refrigerated Truck",
            "Livestock Transport Truck",
            "Grain Hauler",
            "Flatbed Truck"
        )

        val productTypes = arrayOf(
            "Select Product Type",
            "Livestock",
            "Vegetables",
            "Fruits",
            "Grains",
            "Dairy Products",
            "Poultry",
            "Fishery Products",
            "Sugarcane",
            "Cotton",
            "Fertilizers & Seeds",
            "Agro-Chemicals",
            "Animal Feed"
        )

        val weights = arrayOf(
            "Select Weight",
            "Less than 500 kg",
            "500 kg - 1 Ton",
            "1-3 Tons",
            "3-5 Tons",
            "5-10 Tons",
            "10-20 Tons",
            "More than 20 Tons"
        )


        vehicleTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)
        productTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, productTypes)
        weightSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, weights)

        findViewById<Button>(R.id.search_button).setOnClickListener {
            val selectedVehicle = vehicleTypeSpinner.selectedItem.toString()
            val selectedProduct = productTypeSpinner.selectedItem.toString()
            val selectedWeight = weightSpinner.selectedItem.toString()

            if (selectedVehicle.contains("Select") || selectedProduct.contains("Select") || selectedWeight.contains("Select")) {
                Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show()
            } else {
                saveDeliveryToFirestore(selectedVehicle, selectedProduct, selectedWeight)
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }
    }

    private fun saveDeliveryToFirestore(vehicleType: String, productType: String, weight: String) {
        val deliveryId = firestore.collection("deliveries").document().id

        val deliveryItem = DeliveryItem(
            id = deliveryId,
            pickupLocation = "Unknown",
            provincePickup = "Unknown",
            destination = "Unknown",
            provinceDestination = "Unknown",
            estimatedTime = "Unknown",
            totalCost = "₱0",
            profileImage = "",
            timestamp = Timestamp.now()
        )

        firestore.collection("deliveries").document(deliveryId)
            .set(deliveryItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Delivery Added Successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Add Delivery", Toast.LENGTH_SHORT).show()
            }
    }
}
