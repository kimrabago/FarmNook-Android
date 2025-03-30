package com.ucb.capstone.farmnook.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Delivery
import java.util.*

class AddDeliveryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fromLocation: TextView
    private lateinit var toLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PICKER_REQUEST = 1
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var productTypeSpinner: Spinner
    private lateinit var weightSpinner: Spinner

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val fromButton: LinearLayout = findViewById(R.id.fromButton)
        val toButton: LinearLayout = findViewById(R.id.toButton)
        fromLocation = findViewById(R.id.from_location)
        toLocation = findViewById(R.id.to_location)

        fromButton.setOnClickListener { openLocationPicker("from") }
        toButton.setOnClickListener { openLocationPicker("to") }

        productTypeSpinner = findViewById(R.id.product_type_spinner)
        weightSpinner = findViewById(R.id.weight_spinner)



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

        checkLocationPermissionAndFetch()
    }

    private fun openLocationPicker(type: String) {
        val intent = Intent(this, LocationPickerActivity::class.java).apply {
            putExtra("location_type", type)
        }
        startActivityForResult(intent, LOCATION_PICKER_REQUEST)
    }

    @SuppressLint("MissingPermission")
    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLatitude = location.latitude
                userLongitude = location.longitude
                val address = getAddressFromCoordinates(userLatitude!!, userLongitude!!)
                fromLocation.text = address ?: "Lat: ${userLatitude}, Lng: ${userLongitude}"
            }
        }
    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else {
            fetchUserLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.get(0)?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveDeliveryToFirestore(vehicleType: String, productType: String, weight: String) {
        val deliveryItem = Delivery(
            pickupLocation = fromLocation.text.toString(),
            destination = toLocation.text.toString(),
            truckType = vehicleType,
            productType = productType,
            weight = weight,
            timestamp = Timestamp.now()
        )

        firestore.collection("deliveries").add(deliveryItem)
            .addOnSuccessListener { Toast.makeText(this, "Delivery Added Successfully!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Failed to Add Delivery", Toast.LENGTH_SHORT).show() }
    }
    private fun updateProductAndWeightOptions(selectedVehicle: String) {
        val productTypes = vehicleProductMap[selectedVehicle] ?: emptyList()
        val weights = vehicleWeightMap[selectedVehicle] ?: emptyList()

        productTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Product Type") + productTypes)
        weightSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Weight") + weights)
    }

}
