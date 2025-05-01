package com.ucb.capstone.farmnook.data.model.algo

data class CostEstimationRequest(
    val vehicleType: String,
    val weight: Double,
    val pickupDistance: Double,
    val deliveryDistance: Double
)