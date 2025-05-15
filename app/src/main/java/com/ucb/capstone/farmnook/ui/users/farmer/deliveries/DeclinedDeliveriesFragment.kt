package com.ucb.capstone.farmnook.ui.users.farmer.deliveries

import android.util.Log

class DeclinedDeliveriesFragment : BaseDeliveryFragment() {
    override fun filterStatus(
        isStarted: Boolean?, isDone: Boolean?, status: String?, isDeclined: Boolean?, isAccepted: Boolean?
    ): Boolean {
        Log.d("DeclinedFilter", "isDeclined=$isDeclined, isAccepted=$isAccepted, status=$status")
        return isDeclined == true
    }
}