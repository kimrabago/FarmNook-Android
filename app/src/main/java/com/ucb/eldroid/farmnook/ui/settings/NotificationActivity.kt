package com.ucb.eldroid.farmnook.ui.settings

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
import com.ucb.eldroid.farmnook.data.model.NotificationItem
import com.ucb.eldroid.farmnook.ui.adapter.NotificationAdapter

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

        // Back button for notifications
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener { finish() }

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
        notificationListener?.remove()
    }
}
