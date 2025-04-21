package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R

class RateDelivery : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentBox: EditText
    private lateinit var rateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_delivery)

        ratingBar = findViewById(R.id.ratingBar)
        commentBox = findViewById(R.id.commentBox)
        rateButton = findViewById(R.id.rate_button)

        val deliveryId = intent.getStringExtra("deliveryId")
        val businessId = intent.getStringExtra("businessId")
        val farmerId = intent.getStringExtra("farmerId")

        rateButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = commentBox.text.toString()

            if (deliveryId.isNullOrEmpty() || businessId.isNullOrEmpty() || farmerId.isNullOrEmpty()) {
                Toast.makeText(this, "Missing data. Cannot submit feedback.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val feedbackRef = db.collection("feedback").document() // Auto-generate doc ID

            val feedback = hashMapOf(
                "feedbackId" to feedbackRef.id, // Match doc ID
                "deliveryId" to deliveryId,
                "farmerId" to farmerId,
                "businessId" to businessId,
                "rating" to rating,
                "comment" to comment,
                "timestamp" to Timestamp.now()
            )

            feedbackRef.set(feedback)
                .addOnSuccessListener {
                    Toast.makeText(this, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("RateDelivery", "Failed to submit feedback", e)
                    Toast.makeText(this, "Error submitting feedback.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
