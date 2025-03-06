package com.ucb.eldroid.farmnook.model.data

import com.google.firebase.Timestamp

data class DeliveryItem(
    var id: String? = null,
    var pickupLocation: String? = null,
    var provincePickup: String? = null,
    var destination: String? = null,
    var provinceDestination: String? = null,
    var estimatedTime: String? = null,
    var totalCost: String? = null,
    var profileImage: String? = null,
    var timestamp: Timestamp? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null, null)
}
