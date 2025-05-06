package com.ucb.capstone.farmnook.data.model

import java.io.Serializable

data class VehicleWithBusiness(
    val vehicleId: String,
    val vehicleType: String,
    val model: String,
    val plateNumber: String,
    val businessName: String,
    val businessId: String,
    val businessLocation: String,
    val locationName: String,
    val profileImage: String? = null,
    val averageRating: Double?,
    var estimatedCost: Double? = null,
) : Serializable