package com.ucb.capstone.farmnook.core

import android.app.Application
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val ONESIGNAL_APP_ID = "4e5673fb-8d4d-4ee6-a268-7fab9d390be7"

class ApplicationClass : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize OneSignal
        Log.d("OneSignal", "🔧 Initializing OneSignal with APP_ID: $ONESIGNAL_APP_ID")
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // Ask for notification permission
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("OneSignal", "📩 Requesting notification permission...")
            OneSignal.Notifications.requestPermission(false)
        }

        // Handle notification click
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                Log.d("OneSignal", "🔔 Notification clicked: ${event.notification.notificationId}")
                val intent = Intent(applicationContext, com.ucb.capstone.farmnook.ui.settings.NotificationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
        })

        // Save playerId to Firestore
        // Save playerId to Firestore based on userType
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val uid = currentUser?.uid

            if (uid != null) {
                // Fetch the user document to get the userType
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val userType = document.getString("userType")
                        // Fetch and update OneSignal playerId
                        val playerId = OneSignal.User.pushSubscription.id
                        Log.d("OneSignal", "Updated playerId for role: $playerId")

// Update Firestore with the new playerId
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null && !playerId.isNullOrEmpty()) {
                            val fieldToUpdate = if (userType == "Hauler") "haulerId" else "farmerId"
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update(
                                    "playerIds",
                                    FieldValue.arrayUnion(playerId),
                                    fieldToUpdate,
                                    uid
                                )
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Successfully updated playerId in Firestore")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Failed to update playerId", e)
                                }
                        }
                    }
            }
        }
    }
}
