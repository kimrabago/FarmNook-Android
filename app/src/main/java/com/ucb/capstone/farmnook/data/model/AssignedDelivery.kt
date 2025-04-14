package com.ucb.capstone.farmnook.data.model

data class AssignedDelivery(
    val deliveryId: String = "",
    val requestId: String = "",
    val haulerAssignedId: String = "",
    val dateJoined: com.google.firebase.Timestamp? = null
)
