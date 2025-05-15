package com.ucb.capstone.farmnook.ui.users.farmer

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.ucb.capstone.farmnook.utils.loadImage

class DeliveryConfirmationActivity : AppCompatActivity() {

    private lateinit var haulerNameTextView: TextView
    private lateinit var profileImage: ImageView
    private lateinit var businessNameTextView: TextView
    private lateinit var plateNumberTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var vehicleTypeTextView: TextView
    private lateinit var modelTextView: TextView
    private lateinit var productTypeTextView: TextView
    private lateinit var capacityTextView: TextView
    private lateinit var receiverTextView: TextView
    private lateinit var proofImageBtn: Button
    private var businessId: String? = null  // NEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_confirmation)

        profileImage = findViewById(R.id.profileImage)
        haulerNameTextView = findViewById(R.id.haulerName)
        businessNameTextView = findViewById(R.id.businessName)
        plateNumberTextView = findViewById(R.id.plateNumber)
        locationTextView = findViewById(R.id.location)
        vehicleTypeTextView = findViewById(R.id.vehicleType)
        modelTextView = findViewById(R.id.model)
        productTypeTextView = findViewById(R.id.productType)
        capacityTextView = findViewById(R.id.capacity)
        receiverTextView = findViewById(R.id.receiverInfo)
        proofImageBtn = findViewById(R.id.viewProofImageBtn)

        val backButton = findViewById<ImageButton>(R.id.btn_back)

        backButton.setOnClickListener { finish() }

        val deliveryId = intent.getStringExtra("deliveryId") ?: run {
            finish()
            return
        }
        val haulerId = intent.getStringExtra("haulerId") ?: run {
            finish()
            return
        }
        val farmerId = intent.getStringExtra("farmerId") ?: run {
            finish()
            return
        }


        fetchDeliveryDetails(deliveryId, haulerId)

        val confirmButton = findViewById<Button>(R.id.confirm_button)
        confirmButton.setOnClickListener {
            if (businessId == null) {
                Log.e("DeliveryConfirmation", "businessId is null, cannot proceed to rate.")
                return@setOnClickListener
            }

            val rateDialog = RateDeliveryDialog.newInstance(deliveryId, businessId!!, farmerId)
            rateDialog.show(supportFragmentManager, "RateDeliveryDialog")
        }

        checkIfAlreadyRated(deliveryId)

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

                val proofImageUrl = deliveryDoc.getString("proofImageUrl")
                val viewProofImageBtn = findViewById<Button>(R.id.viewProofImageBtn)

                if (!proofImageUrl.isNullOrEmpty()) {
                    viewProofImageBtn.visibility = View.VISIBLE
                    viewProofImageBtn.setOnClickListener {
                        showProofImageDialog(proofImageUrl)
                    }
                } else {
                    viewProofImageBtn.visibility = View.GONE
                }

                fetchRequestDetails(requestId)
                fetchHaulerDetails(haulerIdFromDoc)
            }
    }

    private fun showProofImageDialog(imageUrl: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_proof_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.proofImageView)


        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setPositiveButton("Close", null)

        val dialog = builder.create()
        dialog.show()

        imageView.loadImage(imageUrl)
    }


    @SuppressLint("SetTextI18n")
    private fun fetchRequestDetails(requestId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("deliveryRequests").document(requestId)
            .addSnapshotListener { requestDoc, error ->
                if (error != null || requestDoc == null || !requestDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to request: ${error?.message}")
                    return@addSnapshotListener
                }

                val productType = requestDoc.getString("productType") ?: ""
                val purpose = requestDoc.getString("purpose")?.replaceFirstChar { it.uppercase() } ?: ""
                val vehicleId = requestDoc.getString("vehicleId") ?: ""
                val destinationLocation = requestDoc.getString("destinationName") ?: ""
                val receiverName = requestDoc.getString("receiverName") ?: ""
                val receiverNum = requestDoc.getString("receiverNumber") ?: ""

                receiverTextView.text = "$receiverName\n$receiverNum"
                productTypeTextView.text = productType
                capacityTextView.text = purpose
                locationTextView.text = destinationLocation

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
            .addSnapshotListener { haulerDoc, error ->
                if (error != null || haulerDoc == null || !haulerDoc.exists()) {
                    Log.e("DeliveryConfirmation", "Failed to listen to hauler: ${error?.message}")
                    return@addSnapshotListener
                }

                val firstName = haulerDoc.getString("firstName") ?: ""
                val lastName = haulerDoc.getString("lastName") ?: ""
                val fullName = "${firstName.replaceFirstChar { it.uppercase() }} ${lastName.replaceFirstChar { it.uppercase() }}"
                haulerNameTextView.text = fullName

                val profileImageUrl = haulerDoc.getString("profileImageUrl") ?: ""
                profileImage.loadImage(profileImageUrl)

                val userType = haulerDoc.getString("userType") ?: ""
                val userId = haulerDoc.getString("userId") ?: ""

                businessId = haulerDoc.getString("businessId")

                if (businessId == null && userType == "Hauler Business Admin") {
                    businessId = userId
                }

                businessId?.let { id ->
                    db.collection("users").document(id)
                        .addSnapshotListener { businessDoc, error ->
                            if (error != null || businessDoc == null || !businessDoc.exists()) {
                                return@addSnapshotListener
                            }

                            val businessName = businessDoc.getString("businessName") ?: ""
                            businessNameTextView.text = businessName
                        }
                }
            }
    }

    private fun checkIfAlreadyRated(deliveryId: String) {
        val db = FirebaseFirestore.getInstance()
        val confirmButton = findViewById<Button>(R.id.confirm_button)

        db.collection("feedback")
            .whereEqualTo("deliveryId", deliveryId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("DeliveryConfirmation", "Error listening for feedback: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    confirmButton.isEnabled = false
                    confirmButton.text = "Confirmed"
                    confirmButton.alpha = 0.5f
                } else {
                    confirmButton.isEnabled = true
                    confirmButton.text = "Confirm"
                    confirmButton.alpha = 1f
                }
            }
    }
}
