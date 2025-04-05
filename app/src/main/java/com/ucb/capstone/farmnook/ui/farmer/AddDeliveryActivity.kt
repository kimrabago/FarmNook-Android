package com.ucb.capstone.farmnook.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Delivery
import java.util.*

@Suppress("DEPRECATION")
class AddDeliveryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fromLocation: TextView
    private lateinit var toLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private lateinit var vehicleTypeSpinner: Spinner
    private lateinit var productTypeSpinner: Spinner
    private lateinit var weightSpinner: Spinner

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    // Maps
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

        // Views Spinners
        val fromButton: LinearLayout = findViewById(R.id.fromButton)
        val toButton: LinearLayout = findViewById(R.id.toButton)
        fromLocation = findViewById(R.id.from_location)
        toLocation = findViewById(R.id.to_location)
        vehicleTypeSpinner = findViewById(R.id.vehicle_type_spinner)
        productTypeSpinner = findViewById(R.id.product_type_spinner)
        weightSpinner = findViewById(R.id.weight_spinner)

        val vehicleTypes = listOf("Select Vehicle Type") + vehicleProductMap.keys
        vehicleTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, vehicleTypes)

        vehicleTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedVehicle = vehicleTypeSpinner.selectedItem.toString()
                if (selectedVehicle != "Select Vehicle Type") {
                    updateProductAndWeightOptions(selectedVehicle)
                }
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
                proceedToRecommendations(selectedVehicle, selectedProduct, selectedWeight)
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }

        checkLocationPermissionAndFetch()
    }

    private fun proceedToRecommendations(vehicleType: String, productType: String, weight: String) {
        val pickupCoordinates = if (userLatitude != null && userLongitude != null)
            "${userLatitude},${userLongitude}" else "0.0,0.0"

        //currentUserID
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid

        val intent = Intent(this, RecommendationActivity::class.java).apply {
            putExtra("pickupLocation", pickupCoordinates)
            putExtra("destinationLocation", "10.331149791236012,123.9112171375494") // static for now
            putExtra("vehicleType", vehicleType)
            putExtra("productType", productType)
            putExtra("farmerId", farmerId)
            putExtra("weight", weight)
        }
        startActivity(intent)
    }

    private fun updateProductAndWeightOptions(selectedVehicle: String) {
        val productTypes = vehicleProductMap[selectedVehicle] ?: emptyList()
        val weights = vehicleWeightMap[selectedVehicle] ?: emptyList()

        productTypeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Product Type") + productTypes)
        weightSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Select Weight") + weights)
    }

    @SuppressLint("MissingPermission")
    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLatitude = location.latitude
                userLongitude = location.longitude
                val address = getAddressFromCoordinates(userLatitude!!, userLongitude!!)
                fromLocation.text = address ?: "$userLatitude, $userLongitude"
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
}
