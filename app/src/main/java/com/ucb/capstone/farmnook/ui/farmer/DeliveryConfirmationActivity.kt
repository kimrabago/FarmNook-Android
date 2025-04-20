package com.ucb.capstone.farmnook.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.util.getAddressFromLatLng
import android.location.Geocoder
import android.widget.Button
import android.widget.ImageButton
import com.ucb.capstone.farmnook.ui.farmer.add_delivery.RateDelivery
import java.util.Locale

class DeliveryConfirmationActivity : AppCompatActivity() {

    private lateinit var haulerNameTextView: TextView
    private lateinit var plateNumberTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var vehicleTypeTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var productTypeTextView: TextView
    private lateinit var capacityTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_confirmation)

        // Bind views
        haulerNameTextView = findViewById(R.id.haulerName)
        plateNumberTextView = findViewById(R.id.plateNumber)
        locationTextView = findViewById(R.id.location)
        vehicleTypeTextView = findViewById(R.id.vehicleType)
        modelTextView = findViewById(R.id.model)
        productTypeTextView = findViewById(R.id.productType)
        capacityTextView = findViewById(R.id.capacity)
        val backButton = findViewById<ImageButton>(R.id.btn_back)

        backButton.setOnClickListener {
            finish()
        }

        val deliveryId = intent.getStringExtra("deliveryId") ?: run {
            Log.e("DeliveryConfirmation", "deliveryId is missing")
            finish()  // Finish the activity if deliveryId is missing
            return
        }
        val haulerId = intent.getStringExtra("haulerId") ?: run {
            Log.e("DeliveryConfirmation", "haulerId is missing")
            finish()  // Finish the activity if haulerId is missing
            return
        }
        val farmerId = intent.getStringExtra("farmerId") ?: run {
            Log.e("DeliveryConfirmation", "farmerId is missing")
            finish()  // Finish the activity if farmerId is missing
            return
        }

        fetchDeliveryDetails(deliveryId, haulerId)

        val confirmButton = findViewById<Button>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            // Navigate to the RateDelivery activity
            val intent = Intent(this, RateDelivery::class.java)
            intent.putExtra("deliveryId", deliveryId)
            intent.putExtra("haulerId", haulerId)
            intent.putExtra("farmerId", farmerId)
            startActivity(intent)
        }
    }

    private fun fetchDeliveryDetails(deliveryId: String, haulerId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("deliveries").document(deliveryId)
            .addSnapshotListener { deliveryDoc, error ->
                if (error != null || deliveryDoc == null || !deliveryDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to delivery: ${error?.message}")
                    return@addSnapshotListener
                }

                val requestId = deliveryDoc.getString("requestId") ?: ""
                val haulerIdFromDoc = deliveryDoc.getString("haulerAssignedId") ?: ""

                fetchRequestDetails(requestId)
                fetchHaulerDetails(haulerIdFromDoc)
            }
    }

    private fun fetchRequestDetails(requestId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("deliveryRequests").document(requestId)
            .addSnapshotListener { requestDoc, error ->
                if (error != null || requestDoc == null || !requestDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to request: ${error?.message}")
                    return@addSnapshotListener
                }

                val productType = requestDoc.getString("productType") ?: ""
                val purpose =
                    requestDoc.getString("purpose")?.replaceFirstChar { it.uppercase() } ?: ""
                val vehicleId = requestDoc.getString("vehicleId") ?: ""
                val destinationLocation = requestDoc.getString("destinationLocation") ?: ""
                val geocoder = Geocoder(this, Locale.getDefault())
                val readableAddress = getAddressFromLatLng(destinationLocation, geocoder)

                productTypeTextView.text = productType
                capacityTextView.text = purpose
                locationTextView.text = readableAddress

                fetchVehicleDetails(vehicleId)
            }
    }

    private fun fetchVehicleDetails(vehicleId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("vehicles").document(vehicleId)
            .addSnapshotListener { vehicleDoc, error ->
                if (error != null || vehicleDoc == null || !vehicleDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to vehicle: ${error?.message}")
                    return@addSnapshotListener
                }

                val plateNumber = vehicleDoc.getString("plateNumber") ?: ""
                val vehicleType = vehicleDoc.getString("vehicleType") ?: ""
                val model = vehicleDoc.getString("model") ?: ""

                plateNumberTextView.text = plateNumber
                vehicleTypeTextView.text = vehicleType
                modelTextView.text = model
            }
    }

    private fun fetchHaulerDetails(haulerId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(haulerId)
            .addSnapshotListener { userDoc, error ->
                if (error != null || userDoc == null || !userDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to hauler: ${error?.message}")
                    return@addSnapshotListener
                }

                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val fullName =
                    "${firstName.replaceFirstChar { it.uppercase() }} ${lastName.replaceFirstChar { it.uppercase() }}"

                haulerNameTextView.text = fullName
            }
    }
}
