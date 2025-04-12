package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.Feedback

class RateDelivery : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentBox: EditText
    private lateinit var rateButton: Button
    private lateinit var closeDialog: ImageView

    private lateinit var deliveryId: String  // To hold the dynamic deliveryId
    private lateinit var farmerId: String    // To hold the farmer's ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_delivery)

        ratingBar = findViewById(R.id.ratingBar)
        commentBox = findViewById(R.id.commentBox)
        rateButton = findViewById(R.id.rate_button)
        closeDialog = findViewById(R.id.closeDialog)

        // Get deliveryId and farmerId from the Intent
        deliveryId = intent.getStringExtra("deliveryId") ?: ""
        farmerId = intent.getStringExtra("farmerId") ?: ""

        // Set RatingBar to be editable
        ratingBar.setIsIndicator(false)

        closeDialog.setOnClickListener {
            finish()
        }

        rateButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = commentBox.text.toString().trim()

            if (comment.isNotEmpty()) {
                // Create the Feedback object
                val feedback = Feedback(
                    feedbackId = "",  // Firestore will generate this
                    rating = rating,
                    comment = comment,
                    deliveryId = deliveryId,
                    farmerId = farmerId,
                    timestamp = System.currentTimeMillis()
                )

                // Add feedback to Firestore and get the feedbackId
                FirebaseFirestore.getInstance()
                    .collection("feedback")
                    .add(feedback)
                    .addOnSuccessListener { documentReference ->
                        val feedbackId = documentReference.id  // Get the feedbackId

                        // Update the feedbackId in Firestore for the current feedback
                        FirebaseFirestore.getInstance()
                            .collection("feedback")
                            .document(feedbackId)
                            .update("feedbackId", feedbackId)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to update feedback ID", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
