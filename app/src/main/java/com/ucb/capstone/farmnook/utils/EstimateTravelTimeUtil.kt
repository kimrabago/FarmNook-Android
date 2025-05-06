package com.ucb.capstone.farmnook.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object EstimateTravelTimeUtil {

    private const val mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    suspend fun getEstimatedTravelTime(pickup: String, drop: String): String =
        suspendCancellableCoroutine { cont ->
            val (pickupLat, pickupLng) = pickup.split(",").map { it.trim().toDouble() }
            val (dropLat, dropLng) = drop.split(",").map { it.trim().toDouble() }

            // ✅ Proper order: longitude,latitude
            val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                    "$pickupLng,$pickupLat;$dropLng,$dropLat" +
                    "?access_token=$mapboxToken&overview=false"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume("Unknown", null)        // ✅ Correct
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val estimated = try {
                        val json = JSONObject(body ?: "")
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val durationSec = routes.getJSONObject(0).getDouble("duration")
                            val minutes = (durationSec / 60).toInt()
                            if (minutes < 60) "$minutes min" else "${minutes / 60} hr ${minutes % 60} mins"
                        } else {
                            "Unknown"
                        }
                    } catch (e: Exception) {
                        "Unknown"
                    }

                    if (cont.isActive) cont.resume(estimated, null)
                }
            })
        }
}