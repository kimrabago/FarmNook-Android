package com.ucb.capstone.farmnook.ui.hauler.subscription

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import java.text.SimpleDateFormat
import java.util.*

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
            if (subscriptionStatusTextView.text.toString() == "Boosted") {
                Toast.makeText(this, "Already boosted", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update UI and show toast
            subscriptionStatusTextView.text = "Boosted"
            Toast.makeText(this, "Subscription successful! You are now boosted.", Toast.LENGTH_SHORT).show()

            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                // Update user's subscription status in Firestore
                db.collection("users").document(userId)
                    .update("subscriptionStatus", "Boosted")
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating user status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                // Generate a unique notification ID
                val notifRef = db.collection("notifications").document()
                val notifId = notifRef.id

                val notificationRecord = hashMapOf(
                    "notifId" to notifId,
                    "userId" to userId,
                    "userName" to userNameTextView.text.toString(),
                    "notifMessage" to "Your subscription has been activated!",
                    "dateTime" to getCurrentTime(),
                    "timestamp" to FieldValue.serverTimestamp()
                )

                notifRef.set(notificationRecord)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Notification logged in database.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to log notification: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Fetch the current user's full name and subscription status from Firestore
    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"
                        userNameTextView.text = fullName

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

    // Helper function to get current time as a formatted string (e.g., "03:45 PM")
    private fun getCurrentTime(): String {
        val currentTime = Calendar.getInstance().time
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(currentTime)
    }
}
