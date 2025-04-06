package com.ucb.capstone.farmnook.ui.settings

import android.os.Bundle
import android.util.Log
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
            .whereEqualTo("farmerId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val notifications = snapshot.documents.mapNotNull { doc ->
                    val ts = try {
                        doc.getTimestamp("timestamp")?.toDate()
                    } catch (e: Exception) {
                        val millis = doc.getLong("timestamp") ?: 0L
                        Date(millis)
                    }
                    Log.d("NotificationActivity", "Data: ${doc.data}")
                    Log.d("NotificationActivity", "Parsing notification: ${doc.id}")
                    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

                    Notification(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestamp = sdf.format(ts)
                    )
                }

                adapter.updateList(notifications)
            }
    }
    private fun clearAllNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .whereEqualTo("farmerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    adapter.updateList(emptyList()) // Clear the list in UI
                }
            }
    }
}