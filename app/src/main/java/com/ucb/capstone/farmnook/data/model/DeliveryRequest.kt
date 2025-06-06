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
    val etaToPickup: String? = null,
    val etaToDestination: String? = null,
    val overallEstimatedTime: Int? = null,
    val estimatedDurationMinutes: Int? = null,
    val scheduledTime: Timestamp? = null,
    var estimatedEndTime: Timestamp? = null,
    val farmerId: String? = null, //the userId
    val businessId: String? = null,
    val vehicleId: String? = null,
    val isAccepted: Boolean? = false,
    val requestId: String? = null,
    val businessName: String? = null,
    val locationName: String? = null,
    val profileImageUrl: String? = null,
    val vehicleType: String? = null,
    val vehicleModel: String? = null,
    val plateNumber: String? = null,
    val receiverName: String? = null,
    val receiverNumber: String? = null,
    val deliveryNote: String? = null,
    val isDone: Boolean = false,
    val isStarted: Boolean = false,
    val isDeclined: Boolean = false,
    val status: String? = null
)