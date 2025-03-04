package com.ucb.eldroid.farmnook.views.message

data class Message(
    val senderName: String,
    val messageContent: String,
    val timestamp: String,
    val avatarResId: Int? = null
)
