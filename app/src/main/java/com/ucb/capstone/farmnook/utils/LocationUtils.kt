package com.ucb.capstone.farmnook.util

import android.location.Geocoder

fun getAddressFromLatLng(locationStr: String?, geocoder: Geocoder): String {
    return try {
        if (!locationStr.isNullOrBlank()) {
            val parts = locationStr.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                val addressList = geocoder.getFromLocation(lat, lng, 1)
                if (!addressList.isNullOrEmpty()) {
                    addressList[0].getAddressLine(0)
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
