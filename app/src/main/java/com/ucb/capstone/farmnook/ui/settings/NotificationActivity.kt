package com.ucb.capstone.farmnook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.data.model.Notification
import com.ucb.capstone.farmnook.databinding.ActivityNotificationBinding
import com.ucb.capstone.farmnook.ui.adapter.NotificationAdapter
import com.ucb.capstone.farmnook.ui.farmer.DeliveryConfirmationActivity
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private val adapter by lazy {
        NotificationAdapter(emptyList()) { notif ->
            // Mark as read
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(notif.id)
                .update("isRead", true)

            val deliveryId = notif.deliveryId
            val farmerId = notif.farmerId

            if (notif.title == "Delivery Completed" && deliveryId != null) {
                // Fetch the delivery document to get the haulerId
                FirebaseFirestore.getInstance()
                    .collection("deliveries")
                    .document(deliveryId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val haulerId = document.getString("AssignedHaulerId") ?: ""
                            val intent = Intent(this, DeliveryConfirmationActivity::class.java).apply {
                                putExtra("deliveryId", deliveryId)
                                putExtra("farmerId", farmerId)
                                putExtra("haulerId", haulerId)
                            }
                            startActivity(intent)
                        } else {
                            Log.e("NotificationActivity", "Delivery document not found for $deliveryId")
                            Toast.makeText(this, "Delivery details not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("NotificationActivity", "Failed to fetch delivery info", exception)
                        Toast.makeText(this, "Failed to fetch delivery info", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // For general notifications without deliveryId
                Toast.makeText(this, notif.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notificationRecyclerView.adapter = adapter

        binding.btnBack.setOnClickListener{
            finish()
        }

        binding.btnDeleteAll.setOnClickListener {
            clearAllNotifications()
        }

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("NotificationActivity", "Current userId: $userId")

        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                Log.d("NotificationDebug", "Snapshot size: ${snapshot.size()}")

                for (doc in snapshot.documents) {
                    Log.d("NotificationDebug", "Doc ID: ${doc.id}, Data: ${doc.data}")
                }

                val notifications = snapshot.documents.mapNotNull { doc ->
                    val ts = try {
                        doc.getTimestamp("timestamp")?.toDate()
                    } catch (e: Exception) {
                        val millis = doc.getLong("timestamp") ?: 0L
                        Date(millis)
                    }
                    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

                    Notification(
                        id = doc.getString("notificationId") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestamp = sdf.format(ts),
                        deliveryId = doc.getString("deliveryId"),
                        farmerId = doc.getString("recipientId")
                    )
                }

                adapter.updateList(notifications)
            }
    }
    private fun clearAllNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    adapter.updateList(emptyList())
                }
            }
    }
}