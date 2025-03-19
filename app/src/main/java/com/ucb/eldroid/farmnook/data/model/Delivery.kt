package com.ucb.eldroid.farmnook.data.model

import com.google.firebase.Timestamp

data class Delivery(
    var id: String? = null,
    var pickupLocation: String? = null,
    var provincePickup: String? = null,
    var destination: String? = null,
    var provinceDestination: String? = null,
    var estimatedTime: String? = null,
    var totalCost: String? = null,
    var profileImage: String? = null,
    var truckType: String? = null,  // Added truck type
    var productType: String? = null, // Added product type
    var weight: String? = null,      // Added weight
    var timestamp: Timestamp? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null, null, null, null, null)
}
