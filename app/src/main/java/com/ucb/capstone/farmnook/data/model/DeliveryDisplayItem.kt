package com.ucb.capstone.farmnook.data.model

data class DeliveryDisplayItem(
    val deliveryId: String,
    val pickupLocation: String,
    val destinationLocation: String,
    val estimatedTime: String
)
