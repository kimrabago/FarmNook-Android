package com.ucb.capstone.farmnook.data.model

data class DeliveryHistory(
    val historyId: String = "",
    val deliveryId: String = "",
    val deliveryArrivalTime: com.google.firebase.Timestamp? = null,
    val distanceTraveled: String = "N/A"
)