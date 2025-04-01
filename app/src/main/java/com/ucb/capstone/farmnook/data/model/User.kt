package com.ucb.capstone.farmnook.data.model

class User(
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val userType: String = "",
    val phoneNum: String? = null,
    val companyName: String? = null,
    val dateJoined: String = "",
    val profileImage: String? = null
)
