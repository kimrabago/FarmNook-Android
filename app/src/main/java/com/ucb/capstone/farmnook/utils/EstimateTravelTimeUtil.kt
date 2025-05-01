package com.ucb.capstone.farmnook.utils

import android.os.Handler
import android.os.Looper
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object EstimateTravelTimeUtil {

    private const val mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    fun getEstimatedTravelTime(pickup: String, drop: String, callback: (String) -> Unit) {
        val (startLat, startLng) = pickup.split(",").map { it.trim() }
        val (endLat, endLng) = drop.split(",").map { it.trim() }

        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$startLng,$startLat;$endLng,$endLat?access_token=$mapboxToken&overview=false"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post { callback("Unknown") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val durationSec = routes.getJSONObject(0).getDouble("duration")
                        val minutes = (durationSec / 60).toInt()
                        val estimated = if (minutes < 60) "$minutes min"
                        else "${minutes / 60} hr ${minutes % 60} min"
                        Handler(Looper.getMainLooper()).post { callback(estimated) }
                    } else {
                        Handler(Looper.getMainLooper()).post { callback("Unknown") }
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post { callback("Unknown") }
                }
            }
        })
    }
}