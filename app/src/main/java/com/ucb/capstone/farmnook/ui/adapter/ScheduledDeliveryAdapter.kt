package com.ucb.capstone.farmnook.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.ScheduledDelivery
import java.text.SimpleDateFormat
import java.util.*

class ScheduledDeliveryAdapter(
    private val scheduledList: List<ScheduledDelivery>
) : RecyclerView.Adapter<ScheduledDeliveryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scheduledTimeLabel: TextView = itemView.findViewById(R.id.scheduledTime)
        val deliveryDateTime: TextView = itemView.findViewById(R.id.deliveryDateTime)
        val deliveryEndDateTime: TextView = itemView.findViewById(R.id.deliveryEndDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val delivery = scheduledList[position]
        holder.scheduledTimeLabel.text = "Scheduled Time"

        val timestamp: Timestamp? = delivery.scheduledTime
        val overallEst: Int = delivery.overallEstimatedTime ?: 0

        if (timestamp != null) {
            val startDate = timestamp.toDate()

            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            holder.deliveryDateTime.text = sdf.format(startDate)

            val endCal = Calendar.getInstance().apply {
                time = startDate
                add(Calendar.MINUTE, overallEst)

                // Round up to next 30-minute interval
                val minutes = get(Calendar.MINUTE)
                if (minutes in 1..29) {
                    set(Calendar.MINUTE, 30)
                } else if (minutes in 31..59) {
                    set(Calendar.MINUTE, 0)
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endFormatted = sdf.format(endCal.time)
            holder.deliveryEndDateTime.text = endFormatted
        } else {
            holder.deliveryDateTime.text = "Not scheduled"
            holder.deliveryEndDateTime.text = "-"
        }
    }

    override fun getItemCount(): Int = scheduledList.size
}
