package com.ucb.eldroid.farmnook.views.hauler.subscription

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.settings.NotificationActivity
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionActivity : AppCompatActivity() {

    // Initialize Firestore instance
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        // Back button logic
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        // One-time subscription button logic
        val subscriptionButton = findViewById<Button>(R.id.subscription_btn)
        subscriptionButton.setOnClickListener {
            // Create notification data with username hardcoded to "FarmNook"
            val notificationMessage = "Your one-time subscription was successful!"
            val notification = hashMapOf(
                "userName" to "FarmNook", // Username is replaced with "FarmNook"
                "notifMessage" to notificationMessage,
                "dateTime" to getCurrentTime(),
                "timestamp" to FieldValue.serverTimestamp() // Helps order notifications
            )

            // Save notification data to Firestore
            db.collection("notifications")
                .add(notification)
                .addOnSuccessListener {
                    Toast.makeText(this, "Subscription successful", Toast.LENGTH_SHORT).show()
                    // Navigate to NotificationActivity to see the new notification
                    val intent = Intent(this, NotificationActivity::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to subscribe: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Helper function to get current time as a formatted string
    private fun getCurrentTime(): String {
        val currentTime = Calendar.getInstance().time
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(currentTime)
    }
}
