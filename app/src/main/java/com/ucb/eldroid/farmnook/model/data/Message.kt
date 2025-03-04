package com.ucb.eldroid.farmnook.model.data

data class Message(
    val senderName: String,
    val messageContent: String,
    val timestamp: String,
    val avatarResId: Int? = null  // Use a drawable resource ID for the avatar if available
)
