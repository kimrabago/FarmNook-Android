package com.ucb.capstone.farmnook.ui.users.farmer.deliveries

class InProgressDeliveriesFragment : BaseDeliveryFragment() {
    override fun filterStatus(isStarted: Boolean?, isDone: Boolean?, status: String?, isDeclined: Boolean?, isAccepted: Boolean?): Boolean {
        return (isAccepted == true || isStarted == true) && isDone != true
    }
}