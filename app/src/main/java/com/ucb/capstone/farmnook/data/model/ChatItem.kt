package com.ucb.capstone.farmnook.data.model


data class ChatItem(
    val chatId: String = "",
    val otherUserId: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)