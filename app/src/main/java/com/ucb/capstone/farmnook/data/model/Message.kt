package com.ucb.capstone.farmnook.data.model


import com.google.firebase.firestore.Exclude


data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val senderName: String = "",
    @get:Exclude var formattedTimestamp: String = ""  // New field for formatted timestamp
)
