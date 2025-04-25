package com.ucb.capstone.farmnook.ui.farmer.add_delivery

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

object SendPushNotification {

    private const val ONE_SIGNAL_APP_ID = "4e5673fb-8d4d-4ee6-a268-7fab9d390be7"
    private const val ONE_SIGNAL_API_KEY = "os_v2_app_jzlhh64njvhonitip6vz2oil46g64bdagwdumafaqquyisuuucapph6jnfwofmjcs3oaauutfhxzdq6sbyu72jgbiktprkewj5uty5y" // Replace with your actual OneSignal REST API key

    fun notifyBusinessOfRequest(context: Context, businessId: String, farmerName: String) {
        val db = FirebaseFirestore.getInstance()

        val notifRef = db.collection("notifications").document()
        val notification = mapOf(
            "notificationId" to notifRef.id,
            "businessId" to businessId,
            "title" to "New Delivery Request",
            "message" to "$farmerName has requested a delivery.",
            "timestamp" to Timestamp.now(),
            "isRead" to false
        )

        notifRef.set(notification)

        db.collection("users").document(businessId).get()
            .addOnSuccessListener { doc ->
                val playerIds = doc.get("playerIds") as? List<String>
                if (!playerIds.isNullOrEmpty()) {
                    sendPushToBusiness(
                        context,
                        playerIds,
                        notification["title"]!!.toString(),
                        notification["message"]!!.toString()
                    )
                } else {
                    Log.w("Push", "🚫 No playerId found for businessId: $businessId")
                }
            }
    }

    private fun sendPushToBusiness(context: Context, playerIds: List<String>, title: String, message: String) {
        val json = JSONObject().apply {
            put("app_id", ONE_SIGNAL_APP_ID)
            put("include_player_ids", JSONArray(playerIds)) // ✅ Send to all devices
            put("headings", JSONObject().put("en", title))
            put("contents", JSONObject().put("en", message))
            put("data", JSONObject().put("openTarget", "BusinessDashboard"))
        }

        val request = object : JsonObjectRequest(
            Request.Method.POST,
            "https://onesignal.com/api/v1/notifications",
            json,
            { response -> Log.d("Push", "✅ Sent to business: $response") },
            { error -> Log.e("Push", "❌ Error sending push: $error") }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(
                    "Authorization" to "Basic $ONE_SIGNAL_API_KEY",
                    "Content-Type" to "application/json"
                )
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    fun sendMessageNotification(context: Context, receiverId: String, senderName: String, message: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(receiverId).get()
            .addOnSuccessListener { doc ->
                val playerIds = doc.get("playerIds") as? List<String>
                if (!playerIds.isNullOrEmpty()) {
                    val json = JSONObject().apply {
                        put("app_id", ONE_SIGNAL_APP_ID)
                        put("include_player_ids", JSONArray(playerIds))
                        put("headings", JSONObject().put("en", "Message from $senderName"))
                        put("contents", JSONObject().put("en", message))
                        put("data", JSONObject().apply {
                            put("openTarget", "MessageActivity")
                            put("receiverId", receiverId)
                            put("receiverName", senderName)
                        })
                    }

                    val request = object : JsonObjectRequest(
                        Request.Method.POST,
                        "https://onesignal.com/api/v1/notifications",
                        json,
                        { response -> Log.d("Push", "✅ Chat push sent: $response") },
                        { error -> Log.e("Push", "❌ Failed to send chat push", error) }
                    ) {
                        override fun getHeaders(): MutableMap<String, String> {
                            return mutableMapOf(
                                "Authorization" to "Basic $ONE_SIGNAL_API_KEY",
                                "Content-Type" to "application/json"
                            )
                        }
                    }

                    Volley.newRequestQueue(context).add(request)
                } else {
                    Log.w("Push", "🚫 No player ID found for user: $receiverId")
                }
            }
    }
    // ✅ Helper to store Firestore + Send Message Notification (to Farmer or Business Admin)
    fun sendCompletedDeliveryNotification(
        recipientField: String, // "farmerId" or "businessId"
        recipientId: String,
        deliveryId: String,
        title: String,
        message: String,
        timestamp: Timestamp,
        context: Context
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val notifRef = firestore.collection("notifications").document()

        val notification = mapOf(
            "notificationId" to notifRef.id,
            recipientField to recipientId,
            "deliveryId" to deliveryId,
            "title" to title,
            "message" to message,
            "timestamp" to timestamp,
            "isRead" to false
        )

        notifRef.set(notification)

        // Reuse existing push sender
        sendMessageNotification(
            context = context,
            receiverId = recipientId,
            senderName = "FarmNook",
            message = message
        )
    }
}