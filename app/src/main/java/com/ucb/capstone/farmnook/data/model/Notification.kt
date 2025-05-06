package com.ucb.capstone.farmnook.data.model

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val isRead: Boolean = false,
    val timestamp: String = "",
    val deliveryId: String? = null,
    val farmerId: String? = null,
)