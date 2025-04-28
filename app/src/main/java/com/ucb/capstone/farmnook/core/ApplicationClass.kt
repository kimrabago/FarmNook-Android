@file:Suppress("DEPRECATION")

package com.ucb.capstone.farmnook.core

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import kotlinx.coroutines.*

const val ONESIGNAL_APP_ID = "4e5673fb-8d4d-4ee6-a268-7fab9d390be7"

class ApplicationClass : Application(), LifecycleObserver {

    private var heartbeatJob: Job? = null
    private val heartbeatInterval = 10_000L

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()

        // 🔐 Initialize Firebase first
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 📍 Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBgn5YSLD0N6p62OrEHqjVqVsfijHdupY8")
        }

        // 🔔 Initialize OneSignal push notification
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }

        // 🎯 Handle notification clicks and routing
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = event.notification.additionalData
                val openTarget = data?.optString("openTarget")

                val intent = when (openTarget) {
                    "MessageActivity" -> {
                        val receiverId = data.optString("receiverId")
                        val receiverName = data.optString("receiverName")
                        Intent(applicationContext, com.ucb.capstone.farmnook.ui.message.MessageActivity::class.java).apply {
                            putExtra("receiverId", receiverId)
                            putExtra("receiverName", receiverName)
                        }
                    }
                    "BusinessDashboard" -> Intent(applicationContext, com.ucb.capstone.farmnook.ui.settings.NotificationActivity::class.java)
                    "HaulerDashboard", "FarmerDashboard" -> Intent(applicationContext, NavigationBar::class.java).apply {
                        putExtra("targetFragment", openTarget)
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
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    userRef.get()
                        .addOnSuccessListener { document ->
                            val playerId = OneSignal.User.pushSubscription.id

                            if (!playerId.isNullOrEmpty()) {
                                val updateMap = hashMapOf<String, Any>(
                                    "playerIds" to FieldValue.arrayUnion(playerId)
                                )

                                userRef.update(updateMap)
                            }
                        }
                }
            }
        }

        // 🧠 Listen for app foreground/background
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // ⚠️ Catch crashes and force status offline
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            auth.currentUser?.uid?.let { uid ->
                firestore.collection("users").document(uid)
                    .update("status", false)
                    .addOnSuccessListener { Log.d("Crash", "💥 Status set to false after crash") }
            }

            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }

    // 🟢 App moves to foreground — start heartbeat if user is hauler
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser ?: return@addAuthStateListener
            val userRef = firestore.collection("users").document(user.uid)

            userRef.get().addOnSuccessListener { doc ->
                val userType = doc.getString("userType")
                if (userType == "Hauler") {
                    Log.d("Heartbeat", "🟢 App foreground — Starting heartbeat")
                    startHeartbeat(user.uid)
                } else {
                    Log.d("Heartbeat", "🔕 Not a hauler — heartbeat skipped")
                }
            }.addOnFailureListener {
                Log.e("Heartbeat", "🔥 Failed to get userType for heartbeat", it)
            }
        }
    }

    // 🔴 App moves to background — stop heartbeat and mark offline
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackground() {
        val user = auth.currentUser ?: return
        stopHeartbeat()

        firestore.collection("users").document(user.uid)
            .update("status", false)
            .addOnSuccessListener { Log.d("Heartbeat", "🔴 App background — Status set to false") }
            .addOnFailureListener { Log.e("Heartbeat", "❌ Failed to set status=false on background", it) }
    }

    // 🧹 Best-effort cleanup on force close / swipe away
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_COMPLETE) {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users").document(userId)
                .update("status", false)
                .addOnSuccessListener { Log.d("Memory", "🧹 App killed — status set to false") }
        }
    }

    // ♻️ Start heartbeat loop every 30s to update `lastSeen`
    private fun startHeartbeat(userId: String) {
        val userRef = firestore.collection("users").document(userId)

        userRef.update(
            mapOf("status" to true, "lastSeen" to Timestamp.now())
        ).addOnSuccessListener {
            Log.d("Heartbeat", "✅ Status set to true for $userId")
        }.addOnFailureListener {
            Log.e("Heartbeat", "❌ Failed to set status=true", it)
        }

        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    userRef.update("lastSeen", Timestamp.now())
                    Log.d("Heartbeat", "🔄 Updated lastSeen for $userId")
                } catch (e: Exception) {
                    Log.e("Heartbeat", "❌ Failed to update lastSeen", e)
                }
                delay(heartbeatInterval)
            }
        }
    }

    // 🛑 Stop heartbeat coroutine when app goes background
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
