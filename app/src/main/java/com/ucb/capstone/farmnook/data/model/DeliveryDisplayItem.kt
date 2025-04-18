package com.ucb.capstone.farmnook.data.model

data class DeliveryDisplayItem(
    val deliveryId: String,
    val pickupLocation: String,           // human-readable
    val destinationLocation: String,      // human-readable
    val rawPickup: String,                // coordinates
    val rawDrop: String,                  // coordinates
    val estimatedTime: String,
    val requestId: String
)

