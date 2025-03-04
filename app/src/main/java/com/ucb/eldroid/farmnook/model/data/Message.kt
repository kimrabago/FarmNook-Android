package com.ucb.eldroid.farmnook.model.data

data class Message(
    val senderName: String,
    val messageContent: String,
    val timestamp: String,
    val avatarResId: Int? = null
)
