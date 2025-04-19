package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R

class RateDelivery : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var ratingValueText: TextView
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

        ratingBar = findViewById(R.id.ratingBar)
        ratingValueText = findViewById(R.id.ratingValueText)
        commentBox = findViewById(R.id.commentBox)
        rateButton = findViewById(R.id.rate_button)
        closeDialog = findViewById(R.id.closeDialog)

        deliveryId = intent.getStringExtra("deliveryId") ?: ""
        farmerId = intent.getStringExtra("farmerId") ?: ""
        haulerId = intent.getStringExtra("haulerId") ?: ""

        closeDialog.setOnClickListener { finish() }

        // Realtime rating display
        ratingValueText.text = "Rating: ${ratingBar.rating.toInt()}"
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            ratingValueText.text = "Rating: ${rating.toInt()}"
        }

        db.collection("users").document(farmerId).get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType")
                if (userType != "Farmer") {
                    Toast.makeText(this, "Only Farmers can leave feedback.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val farmerName = "${document.getString("firstName")} ${document.getString("lastName")}"

                rateButton.setOnClickListener {
                    val comment = commentBox.text.toString().trim()
                    val rating = ratingBar.rating.toInt()

                    if (rating == 0) {
                        Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (comment.isEmpty()) {
                        Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val feedbackData = hashMapOf(
                        "feedbackId" to "",
                        "rating" to rating,
                        "comment" to comment,
                        "deliveryId" to deliveryId,
                        "farmerId" to farmerId,
                        "farmerName" to farmerName,
                        "haulerId" to haulerId,
                        "timestamp" to Timestamp.now()
                    )

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
