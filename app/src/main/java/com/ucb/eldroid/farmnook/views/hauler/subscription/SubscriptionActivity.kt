package com.ucb.eldroid.farmnook.views.hauler.subscription

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Views in the layout
    private lateinit var userNameTextView: TextView
    private lateinit var subscriptionStatusTextView: TextView
    private lateinit var subscriptionButton: Button
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find views by ID
        userNameTextView = findViewById(R.id.user_name)
        subscriptionStatusTextView = findViewById(R.id.subscription_status)
        subscriptionButton = findViewById(R.id.subscription_btn)
        backButton = findViewById(R.id.btn_back)

        // Set up back button
        backButton.setOnClickListener { finish() }

        // Fetch user data from Firestore (including subscription status)
        fetchUserData()

        // Handle subscription button click
        subscriptionButton.setOnClickListener {
            // Update the UI to show the new status
            subscriptionStatusTextView.text = "Boosted"
            Toast.makeText(this, "Subscription successful! You are now boosted.", Toast.LENGTH_SHORT).show()

            // Get the current user's ID
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                // Update the user's subscription status in the "users" collection
                db.collection("users").document(userId)
                    .update("subscriptionStatus", "Boosted")
                    .addOnSuccessListener {
                        // Optionally log success or update further UI if needed
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating user status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                // Prepare a subscription record to log in the database
                val subscriptionRecord = hashMapOf(
                    "userId" to userId,
                    "userName" to userNameTextView.text.toString(),
                    "subscriptionStatus" to "Boosted",
                    "timestamp" to FieldValue.serverTimestamp()
                )

                // Add the subscription record to the "subscriptions" collection
                db.collection("subscriptions")
                    .add(subscriptionRecord)
                    .addOnSuccessListener {
                        // Optionally inform the user or log success
                        Toast.makeText(this, "Subscription logged in database.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to log subscription: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Fetch the current user's first and last name and subscription status from Firestore
    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Fetch and display full name
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        userNameTextView.text = fullName

                        // Fetch subscription status (default to "Unboosted" if not set)
                        val status = document.getString("subscriptionStatus") ?: "Unboosted"
                        subscriptionStatusTextView.text = status
                    } else {
                        userNameTextView.text = "User"
                        subscriptionStatusTextView.text = "Unboosted"
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to load user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            userNameTextView.text = "User"
            subscriptionStatusTextView.text = "Unboosted"
        }
    }
}
