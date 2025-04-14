package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.algo.RecommendationRequest
import com.ucb.capstone.farmnook.data.model.algo.RecommendationResponse
import com.ucb.capstone.farmnook.data.service.ApiService
import com.ucb.capstone.farmnook.ui.farmer.LocationPickerActivity
import com.ucb.capstone.farmnook.utils.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

@Suppress("DEPRECATION")
class AddDeliveryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fromLocation: TextView
    private lateinit var toLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var purposeSpinner: Spinner
    private lateinit var productTypeEditText: EditText
    private lateinit var weightEditText: EditText
    private lateinit var pickUpLocButton: LinearLayout
    private lateinit var destinationButton: LinearLayout

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null
    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private val PICKUP_LOCATION_REQUEST = 2001
        private val DESTINATION_LOCATION_REQUEST = 2002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_delivery)

        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Views Spinners
        pickUpLocButton= findViewById(R.id.fromButton)
        destinationButton= findViewById(R.id.toButton)
        fromLocation = findViewById(R.id.from_location)
        toLocation = findViewById(R.id.to_location)
        purposeSpinner = findViewById(R.id.purposeSpinner)
        productTypeEditText = findViewById(R.id.productTypeEditText)
        weightEditText = findViewById(R.id.weightEditText)

        pickUpLocButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            startActivityForResult(intent, PICKUP_LOCATION_REQUEST)
        }

        destinationButton.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            startActivityForResult(intent, DESTINATION_LOCATION_REQUEST)
        }

        // Create a list of items for the dropdown
        val options = listOf("Select Purpose", "Livestock", "Crops", "Perishable Crops")

        purposeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)


        findViewById<Button>(R.id.proceedButton).setOnClickListener {
            val selectedPurpose = purposeSpinner.selectedItem.toString().lowercase()
            val inputtedProduct = productTypeEditText.getText().toString().trim()
            val inputtedWeight = weightEditText.getText().toString().trim()

            if (selectedPurpose == "Select Purpose") {
                Toast.makeText(this, "Please select a purpose", Toast.LENGTH_SHORT).show()
            } else if (TextUtils.isEmpty(inputtedProduct) || TextUtils.isEmpty(inputtedWeight)) {
                Toast.makeText(this, "Please enter both product type and weight", Toast.LENGTH_SHORT).show()
            } else if (destinationLatitude == null || destinationLongitude == null) {
                Toast.makeText(this, "Please select a destination location.", Toast.LENGTH_SHORT).show()
            } else {
                proceedToRecommendations(selectedPurpose, inputtedProduct, inputtedWeight)
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }

        checkLocationPermissionAndFetch()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val selectedLocation = data?.getStringExtra("selectedLocation")
            val selectedCoordinates = data?.getStringExtra("selectedCoordinates")

            if (!selectedLocation.isNullOrEmpty() && !selectedCoordinates.isNullOrEmpty()) {
                val coords = selectedCoordinates.split(",")
                if (coords.size == 2) {
                    when (requestCode) {
                        PICKUP_LOCATION_REQUEST -> {
                            fromLocation.text = selectedLocation
                            userLatitude = coords[0].toDoubleOrNull()
                            userLongitude = coords[1].toDoubleOrNull()
                        }
                        DESTINATION_LOCATION_REQUEST -> {
                            toLocation.text = selectedLocation
                            destinationLatitude = coords[0].toDoubleOrNull()
                            destinationLongitude = coords[1].toDoubleOrNull()
                        }
                    }
                }
            }
        }
    }

    private fun proceedToRecommendations(purpose: String, productType: String, weight: String) {
        val pickupCoordinates = if (userLatitude != null && userLongitude != null)
            "${userLatitude},${userLongitude}" else "0.0,0.0"

        val destinationCoordinates = if (destinationLatitude != null && destinationLongitude != null)
            "${destinationLatitude},${destinationLongitude}" else "0.0,0.0"

        val farmerId = FirebaseAuth.getInstance().currentUser?.uid

        val testRequest = RecommendationRequest(productType, weight.toInt(), purpose)
        val retrofit = RetrofitClient.instance
        val apiService = retrofit?.create(ApiService::class.java)

        apiService?.getRecommendation(testRequest)?.enqueue(object : Callback<RecommendationResponse> {
            override fun onResponse(call: Call<RecommendationResponse>, response: Response<RecommendationResponse>) {
                if (response.isSuccessful) {
                    val rec = response.body()
                    Toast.makeText(this@AddDeliveryActivity, "✅ Recommended: ${rec?.vehicleType}", Toast.LENGTH_LONG).show()

                    // ✅ start RecommendationActivity only after we get the result
                    val intent = Intent(this@AddDeliveryActivity, RecommendationActivity::class.java).apply {
                        putExtra("pickupLocation", pickupCoordinates)
                        putExtra("destinationLocation", destinationCoordinates)
                        putExtra("purpose", purpose)
                        putExtra("productType", productType)
                        putExtra("farmerId", farmerId)
                        putExtra("weight", weight)
                        putExtra("recommendedType", rec?.vehicleType) // ✅ include recommended type here
                    }
                    startActivity(intent)

                } else {
                    Toast.makeText(this@AddDeliveryActivity, "❌ Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecommendationResponse>, t: Throwable) {
                Toast.makeText(this@AddDeliveryActivity, "🚫 Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
