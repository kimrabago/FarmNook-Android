package com.ucb.capstone.farmnook.ui.users.farmer.deliveries

class CancelledDeliveriesFragment : BaseDeliveryFragment() {
    override fun filterStatus(
        isStarted: Boolean?,
        isDone: Boolean?,
        status: String?,
        isDeclined: Boolean?,
        isAccepted: Boolean?
    ): Boolean {
        return status?.equals("Cancelled", ignoreCase = true) == true
    }
}