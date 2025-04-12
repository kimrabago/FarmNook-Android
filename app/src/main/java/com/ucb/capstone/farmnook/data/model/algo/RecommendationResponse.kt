package com.ucb.capstone.farmnook.data.model.algo

import com.google.gson.annotations.SerializedName

data class RecommendationResponse(
    @SerializedName("vehicle_category")
    val vehicleCategory: String,

    @SerializedName("vehicle_type")
    val vehicleType: String
)
