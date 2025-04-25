package com.ucb.capstone.farmnook.data.model

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val senderName: String = "",
    val formattedTimestamp: String = ""  // New field for formatted timestamp
)
