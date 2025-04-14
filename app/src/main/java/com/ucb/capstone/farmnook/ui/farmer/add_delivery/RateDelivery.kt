package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R

class RateDelivery : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentBox: EditText
    private lateinit var rateButton: Button
    private lateinit var closeDialog: ImageView

    private lateinit var deliveryId: String
    private lateinit var farmerId: String
    private lateinit var haulerId: String

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_delivery)

        // Initialize views
        ratingBar = findViewById(R.id.ratingBar)
        commentBox = findViewById(R.id.commentBox)
        rateButton = findViewById(R.id.rate_button)
        closeDialog = findViewById(R.id.closeDialog)

        // Get IDs from intent
        deliveryId = intent.getStringExtra("deliveryId") ?: ""
        farmerId = intent.getStringExtra("farmerId") ?: ""
        haulerId = intent.getStringExtra("haulerId") ?: ""

        // Close dialog
        closeDialog.setOnClickListener { finish() }

        // Fetch farmer's name
        db.collection("users").document(farmerId).get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType")
                if (userType != "Farmer") {
                    Toast.makeText(this, "Only Farmers can leave feedback.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val farmerName = "${document.getString("firstName")} ${document.getString("lastName")}"

                // Handle rating submission
                rateButton.setOnClickListener {
                    val comment = commentBox.text.toString().trim()

                    if (comment.isEmpty()) {
                        Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Prepare feedback data
                    val feedbackData = hashMapOf(
                        "feedbackId" to "", // will be set after `.add()`
                        "rating" to ratingBar.rating,
                        "comment" to comment,
                        "deliveryId" to deliveryId,
                        "farmerId" to farmerId,
                        "farmerName" to farmerName,  // Save the full name
                        "haulerId" to haulerId,
                        "timestamp" to Timestamp.now()
                    )

                    // Submit feedback to Firestore
                    db.collection("feedback")
                        .add(feedbackData)
                        .addOnSuccessListener { docRef ->
                            db.collection("feedback").document(docRef.id)
                                .update("feedbackId", docRef.id)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to verify user", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
