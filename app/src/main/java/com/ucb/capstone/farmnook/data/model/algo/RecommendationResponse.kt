package com.ucb.capstone.farmnook.data.model.algo

import com.google.gson.annotations.SerializedName

data class RecommendationResponse(
    @SerializedName("recommended_vehicle_types")
    val recommendedVehicleTypes: List<String>
)
