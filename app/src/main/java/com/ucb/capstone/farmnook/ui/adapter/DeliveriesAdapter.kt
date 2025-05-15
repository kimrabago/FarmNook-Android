package com.ucb.capstone.farmnook.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.ui.message.MessageActivity
import com.ucb.capstone.farmnook.utils.loadImage
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveriesAdapter(
    private val deliveries: List<DeliveryRequest>,
    private val onItemClick: (DeliveryRequest) -> Unit
) : RecyclerView.Adapter<DeliveriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deliveryId: TextView = view.findViewById(R.id.deliveryIdText)
        val deliveryStatus: TextView = view.findViewById(R.id.deliveryStatusText)
        val haulerName: TextView = view.findViewById(R.id.haulerNameText)
        val vehicleInfo: TextView = view.findViewById(R.id.vehicleInfoText)
        val pickupLocation: TextView = view.findViewById(R.id.pickupLocationText)
        val destinationLocation: TextView = view.findViewById(R.id.destinationLocationText)
        val viewSummaryButton: Button = view.findViewById(R.id.viewSummaryButton)
        val haulerProfileImage: CircleImageView = view.findViewById(R.id.haulerProfileImage)
        val scheduleTime: TextView  = view.findViewById(R.id.scheduledTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_status, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val delivery = deliveries[position]

        holder.deliveryId.text = "Delivery #${delivery.requestId?.take(8)}"
        holder.pickupLocation.text = "From: ${delivery.pickupName}"
        holder.destinationLocation.text = "To: ${delivery.destinationName}"

        // Combine vehicleType + model
        val vehicleText = listOfNotNull(delivery.vehicleType, delivery.vehicleModel)
            .joinToString(" • ")
            .ifEmpty { "Vehicle Info Unavailable" }

        holder.vehicleInfo.text = vehicleText
        Log.d("DeliveriesAdapter", "🚚 Displaying vehicleInfo text: $vehicleText")

        holder.haulerName.text = delivery.businessName
        holder.haulerProfileImage.loadImage(delivery.profileImageUrl)

        val timestamp = delivery.scheduledTime
        val formattedTime = timestamp?.toDate()?.let {
            SimpleDateFormat("MMMM dd, yyyy : hh:mm a", Locale.getDefault()).format(it)
        } ?: "Not Scheduled"

        holder.scheduleTime.text = "Schedule Time: $formattedTime"

        val contextt = holder.itemView.context
        val redColor = ContextCompat.getColor(contextt, R.color.red)
        val defaultBg = ContextCompat.getDrawable(contextt, R.drawable.green_rounded_button) // Make sure this exists or replace accordingly

        when {
            delivery.isDeclined -> {
                holder.viewSummaryButton.apply {
                    text = "Declined"
                    isEnabled = false
                    setBackgroundColor(redColor)
                }
            }
            delivery.status?.equals("Cancelled", ignoreCase = true) == true -> {
                holder.viewSummaryButton.apply {
                    text = "Cancelled"
                    isEnabled = false
                    setBackgroundColor(redColor)
                }
            }
            else -> {
                holder.viewSummaryButton.apply {
                    text = "View Summary"
                    isEnabled = true
                    background = defaultBg
                    setOnClickListener { onItemClick(delivery) }
                }
            }
        }

        holder.deliveryStatus.text = when {
            delivery.isDone -> "Completed"
            delivery.isStarted -> "In Progress"
            delivery.isAccepted == true -> "Accepted"
            delivery.isDeclined -> "Declined"
            delivery.status?.equals("Cancelled", ignoreCase = true) == true -> "Cancelled"
            else -> "Pending"
        }

        // 🔴 Set text color for special cases
        val context = holder.itemView.context
        val red = ContextCompat.getColor(context, R.color.red)
        val green = ContextCompat.getColor(context, R.color.dark_green)

        holder.deliveryStatus.setTextColor(
            when {
                delivery.isDeclined -> red
                delivery.status?.equals("Cancelled", ignoreCase = true) == true -> red
                else -> green
            }
        )
    }

    private fun handleMessageButtonClick(delivery: DeliveryRequest, context: Context) {
        val farmerId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("deliveries")
            .whereEqualTo("requestId", delivery.requestId)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val deliveryDoc = querySnapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                val haulerId = deliveryDoc.getString("haulerAssignedId") ?: return@addOnSuccessListener

                val chatId = if (farmerId < haulerId) "$farmerId-$haulerId" else "$haulerId-$farmerId"

                FirebaseFirestore.getInstance().collection("users").document(haulerId).get()
                    .addOnSuccessListener { haulerDoc ->
                        val firstName = haulerDoc.getString("firstName") ?: ""
                        val lastName = haulerDoc.getString("lastName") ?: ""
                        val haulerName = "$firstName $lastName".trim()

                        Intent(context, MessageActivity::class.java).apply {
                            putExtra("chatId", chatId)
                            putExtra("recipientId", haulerId)
                            putExtra("receiverName", haulerName)
                        }.also { context.startActivity(it) }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to load hauler details", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load delivery details", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = deliveries.size
}