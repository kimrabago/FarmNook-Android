package com.ucb.capstone.farmnook.data.model.algo

import com.google.gson.annotations.SerializedName

data class RecommendationRequest(
    @SerializedName("Product Type")
    val productType: String,

    @SerializedName("Product Weight (kg)")
    val weight: Int,

    @SerializedName("Purpose")
    val purpose: String
)
