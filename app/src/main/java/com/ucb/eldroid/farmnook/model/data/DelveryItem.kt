package com.ucb.eldroid.farmnook.model.data

data class DeliveryItem(
    val id: Int,  // Unique identifier for the delivery
    val pickupLocation: String,
    val provincePickup: String,  // Additional field for the pickup province
    val destination: String,
    val provinceDestination: String,  // Additional field for the destination province
    val estimatedTime: String,
    val totalCost: String,
    val profileImage: String  // URL or path to the profile image
)