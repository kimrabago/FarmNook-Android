package com.ucb.capstone.farmnook.data.model

import com.google.firebase.Timestamp

data class ScheduledDelivery(
    val scheduledTime: Timestamp? = null,
    val overallEstimatedTime: Int? = null,
    val vehicleId: String? = null,
    val pickupLocation: String? = null,
    val destinationLocation: String? = null,
    val purpose: String? = null,
    val productType: String? = null,
    val weight: String? = null,
    val receiverName: String? = null,
    val receiverNumber: String? = null
)