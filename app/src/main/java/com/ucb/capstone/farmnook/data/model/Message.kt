package com.ucb.capstone.farmnook.data.model

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    var senderName: String = "" // Add sender's name
)
