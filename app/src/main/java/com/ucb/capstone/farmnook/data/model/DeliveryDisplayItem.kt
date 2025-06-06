package com.ucb.capstone.farmnook.data.model

import com.google.firebase.Timestamp

data class DeliveryDisplayItem(
    val deliveryId: String,
    val pickupLocation: String,           // human-readable
    val destinationLocation: String,      // human-readable
    val rawPickup: String,                // coordinates
    val rawDrop: String,                  // coordinates
    val estimatedTime: String,
    val totalCost: String,
    val requestId: String,
    val receiverName: String,
    val receiverNum: String,
    val deliveryNote: String? = null,
    val isStarted: Boolean,
    val scheduledTime: Timestamp? = null
)

