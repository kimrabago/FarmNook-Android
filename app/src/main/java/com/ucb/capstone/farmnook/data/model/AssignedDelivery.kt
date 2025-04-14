package com.ucb.capstone.farmnook.data.model

data class AssignedDelivery(
    val deliveryId: String = "",
    val requestId: String = "",
    val haulerAssignedId: String = "",
    val createdAt: com.google.firebase.Timestamp? = null
)
