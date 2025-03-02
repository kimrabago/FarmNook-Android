package com.ucb.eldroid.farmnook.views.settings

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.adapter.NotificationAdapter

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()

    // Firestore instance and listener registration
    private val db = FirebaseFirestore.getInstance()
    private var notificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        // Set up the back button to finish the activity when clicked
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.notification_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        // Listen for notifications ordered by timestamp (most recent first)
        notificationListener = db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("NotificationActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    notificationList.clear()
                    for (doc in snapshots) {
                        // Use "FarmNook" as default username if the field is missing
                        val userName = doc.getString("userName") ?: "FarmNook"
                        val notifMessage = doc.getString("notifMessage") ?: ""
                        val dateTime = doc.getString("dateTime") ?: ""
                        val notification = NotificationItem(userName, notifMessage, dateTime)
                        notificationList.add(notification)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the Firestore listener when the activity is destroyed
        notificationListener?.remove()
    }
}
