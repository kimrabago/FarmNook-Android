package com.ucb.capstone.farmnook.data.model

import com.google.firebase.Timestamp

data class DeliveryRequest(
    val id: String? = null,
    val purpose: String? = null,
    val productType: String? = null,
    val weight: String? = null, // string instead of Int
    val timestamp: Timestamp? = null,
    val pickupLocation: String? = null,
    val destinationLocation: String? = null,
    val estimateCost: String? = null,
    val estimatedTime: String? = null,
    val scheduledTime: String? = null,
    val farmerId: String? = null, //the userId
    val businessId: String? = null,
    val vehicleID: String? = null,
    val isAccepted: Boolean? = false,
    // val profileImage: String? = null // if used
)