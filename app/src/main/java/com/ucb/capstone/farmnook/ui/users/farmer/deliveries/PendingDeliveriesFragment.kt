package com.ucb.capstone.farmnook.ui.users.farmer.deliveries

class PendingDeliveriesFragment : BaseDeliveryFragment() {
    override fun filterStatus(isStarted: Boolean?, isDone: Boolean?, status: String?, isDeclined: Boolean?, isAccepted: Boolean?): Boolean {
        return isStarted != true && isDone != true && status != "Cancelled" && isDeclined != true && isAccepted != true
    }
}