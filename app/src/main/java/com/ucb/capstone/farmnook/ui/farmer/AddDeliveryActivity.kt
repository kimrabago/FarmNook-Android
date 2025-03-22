package com.ucb.capstone.farmnook.ui.farmer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Delivery

class AddDeliveryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fromLocation: TextView
    private lateinit var toLocation: TextView

    companion object {
        private const val LOCATION_PICKER_REQUEST = 1
    }

    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var productTypeSpinner: Spinner
    private lateinit var weightSpinner: Spinner

    private val vehicleProductMap = mapOf(
        "Small Farm Truck" to listOf("Vegetables", "Fruits", "Poultry", "Animal Feed"),
        "Medium Farm Truck" to listOf("Vegetables", "Fruits", "Grains", "Poultry", "Dairy Products"),
        "Large Farm Truck" to listOf("Livestock", "Grains", "Dairy Products", "Fertilizers & Seeds"),
        "Tractor with Trailer" to listOf("Grains", "Sugarcane", "Cotton", "Fertilizers & Seeds"),
        "Pickup Truck" to listOf("Vegetables", "Fruits", "Fishery Products", "Agro-Chemicals"),
        "Refrigerated Truck" to listOf("Dairy Products", "Fishery Products", "Poultry"),
        "Livestock Transport Truck" to listOf("Livestock", "Poultry"),
        "Grain Hauler" to listOf("Grains"),
        "Flatbed Truck" to listOf("Cotton", "Fertilizers & Seeds", "Agro-Chemicals")
    )

    private val vehicleWeightMap = mapOf(
        "Small Farm Truck" to listOf("Up to 500 kg", "500 - 1000 kg"),
        "Medium Farm Truck" to listOf("1000 - 3000 kg", "3000 - 5000 kg"),
        "Large Farm Truck" to listOf("5000 - 10000 kg", "10000 - 20000 kg"),
        "Tractor with Trailer" to listOf("10000 - 20000 kg", "More than 20000 kg"),
        "Pickup Truck" to listOf("Up to 500 kg", "500 - 1000 kg"),
        "Refrigerated Truck" to listOf("1000 - 3000 kg", "3000 - 5000 kg"),
        "Livestock Transport Truck" to listOf("5000 - 10000 kg", "10000 - 20000 kg"),
        "Grain Hauler" to listOf("10000 - 20000 kg", "More than 20000 kg"),
        "Flatbed Truck" to listOf("5000 - 10000 kg", "10000 - 20000 kg", "More than 20000 kg")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delivery)

        firestore = FirebaseFirestore.getInstance()

        val fromButton: LinearLayout = findViewById(R.id.fromButton)
        val toButton: LinearLayout = findViewById(R.id.toButton)
        fromLocation = findViewById(R.id.from_location)
        toLocation = findViewById(R.id.to_location)

        fromButton.setOnClickListener { openLocationPicker("from") }
        toButton.setOnClickListener { openLocationPicker("to") }

        vehicleTypeSpinner = findViewById(R.id.vehicle_type_spinner)
        productTypeSpinner = findViewById(R.id.product_type_spinner)
        weightSpinner = findViewById(R.id.weight_spinner)

        val vehicleTypes = listOf("Select Vehicle Type") + vehicleProductMap.keys

        vehicleTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)

        vehicleTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedVehicle = vehicleTypeSpinner.selectedItem.toString()
                updateProductAndWeightOptions(selectedVehicle)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.search_button).setOnClickListener {
            val selectedVehicle = vehicleTypeSpinner.selectedItem.toString()
            val selectedProduct = productTypeSpinner.selectedItem.toString()
            val selectedWeight = weightSpinner.selectedItem.toString()

            if (selectedVehicle == "Select Vehicle Type" || selectedProduct == "Select Product Type" || selectedWeight == "Select Weight") {
                Toast.makeText(this, "Please select all options", Toast.LENGTH_SHORT).show()
            } else {
                saveDeliveryToFirestore(selectedVehicle, selectedProduct, selectedWeight)
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }
    }
    private fun openLocationPicker(type: String) {
        val intent = Intent(this, LocationPickerActivity::class.java).apply {
            putExtra("location_type", type)
        }
        startActivityForResult(intent, LOCATION_PICKER_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let {
                val locationType = it.getStringExtra("location_type")
                val selectedLocation = it.getStringExtra("selected_location")

                when (locationType) {
                    "from" -> fromLocation.text = selectedLocation
                    "to" -> toLocation.text = selectedLocation
                }
            }
        }
    }

    private fun updateProductAndWeightOptions(selectedVehicle: String) {
        val productTypes = vehicleProductMap[selectedVehicle] ?: emptyList()
        val weights = vehicleWeightMap[selectedVehicle] ?: emptyList()

        productTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Product Type") + productTypes)
        weightSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Weight") + weights)
    }

    private fun saveDeliveryToFirestore(vehicleType: String, productType: String, weight: String) {
        val deliveryId = firestore.collection("deliveries").document().id

        val deliveryItem = Delivery(
            id = deliveryId,
            pickupLocation = "Unknown",
            provincePickup = "Unknown",
            destination = "Unknown",
            provinceDestination = "Unknown",
            estimatedTime = "Unknown",
            totalCost = "₱0",
            profileImage = "",
            truckType = vehicleType,
            productType = productType,
            weight = weight,
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
