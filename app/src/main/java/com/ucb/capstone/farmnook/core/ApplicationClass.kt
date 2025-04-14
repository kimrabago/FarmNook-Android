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
                val data = event.notification.additionalData
                val openTarget = data?.optString("openTarget")

                val intent = when (openTarget) {
                    "MessageActivity" -> {
                        // Extract receiverId and receiverName if you added them in data
                        val receiverId = data.optString("receiverId")
                        val receiverName = data.optString("receiverName")
                        Intent(applicationContext, com.ucb.capstone.farmnook.ui.message.MessageActivity::class.java).apply {
                            putExtra("receiverId", receiverId)
                            putExtra("receiverName", receiverName)
                        }
                    }
                    "BusinessDashboard" -> {
                        Intent(applicationContext, com.ucb.capstone.farmnook.ui.settings.NotificationActivity::class.java)
                    }
                    else -> null
                }

                intent?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(this)
                }
            }
        })

        // Save playerId to Firestore based on userType
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val user = auth.currentUser
                val uid = user?.uid

                if (uid != null) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            val userType = document.getString("userType")
                            val playerId = OneSignal.User.pushSubscription.id

                            if (!playerId.isNullOrEmpty()) {
                                val fieldToUpdate = if (userType == "Hauler") "haulerId" else "farmerId"
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(uid)
                                    .update(
                                        "playerIds", FieldValue.arrayUnion(playerId),
                                        fieldToUpdate, uid
                                    )
                                    .addOnSuccessListener {
                                        Log.d("Firestore", "✅ Updated playerId: $playerId")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "❌ Failed to update playerId", e)
                                    }
                            } else {
                                Log.w("OneSignal", "⚠️ playerId is null or not ready yet.")
                            }
                        }
                } else {
                    Log.d("Auth", "🔒 No authenticated user.")
                }
            }
        }

    }
}
