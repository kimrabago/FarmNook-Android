package com.ucb.capstone.farmnook.data.model

import com.ucb.capstone.farmnook.data.model.User

data class UserProfile(
    val user: User,
    val profileImageUrl: String? = null,
    val businessId: String? = null,
    val businessName: String? = null
)