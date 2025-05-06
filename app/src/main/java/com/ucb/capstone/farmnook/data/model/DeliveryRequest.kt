package com.ucb.capstone.farmnook.data.model

import com.google.firebase.Timestamp

data class DeliveryRequest(
    val id: String? = null,
    val purpose: String? = null,
    val productType: String? = null,
    val weight: String? = null,
    val timestamp: Timestamp? = null,
    val pickupLocation: String? = null,
    val pickupName: String? = null,
    val destinationLocation: String? = null,
    val destinationName: String? = null,
    val estimatedCost: Double? = null,
    val estimatedTime: String? = null,
    val estimatedDurationMinutes: Int? = null,
    val scheduledTime: String? = null,
    val farmerId: String? = null, //the userId
    val businessId: String? = null,
    val vehicleID: String? = null,
    val isAccepted: Boolean? = false,
    val requestId: String? = null,
    val businessName: String? = null,
    val locationName: String? = null,
    val profileImageUrl: String? = null,
    val vehicleType: String? = null,
    val vehicleModel: String? = null,
    val plateNumber: String? = null
)