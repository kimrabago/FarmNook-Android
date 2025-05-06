package com.ucb.capstone.farmnook.util

import android.location.Geocoder

fun getAddressFromLatLng(locationStr: String?, geocoder: Geocoder): String {
    return try {
        if (!locationStr.isNullOrBlank()) {
            val parts = locationStr.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                val addressList = geocoder.getFromLocation(lat, lng, 5) // try more results

                if (!addressList.isNullOrEmpty()) {
                    var bestAddress = "Unknown location"

                    for (address in addressList) {
                        val line = address.getAddressLine(0) ?: continue

                        // Skip if the address looks like a Plus Code
                        if (line.matches(Regex("^[0-9A-Z]{4,}\\+.*"))) continue

                        bestAddress = line
                        break
                    }

                    bestAddress
                } else {
                    "Unknown location"
                }
            } else {
                "Invalid coordinates"
            }
        } else {
            "Not specified"
        }
    } catch (e: Exception) {
        "Error resolving address"
    }
}
