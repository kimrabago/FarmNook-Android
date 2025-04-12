package com.ucb.capstone.farmnook.data.model

data class Feedback(
    val feedbackId: String = "",
    val rating: Float = 0.0f,
    val comment: String = "",
    val deliveryId: String = "",
    val farmerId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
