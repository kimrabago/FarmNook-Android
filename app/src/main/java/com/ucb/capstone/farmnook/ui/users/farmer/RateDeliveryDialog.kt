package com.ucb.capstone.farmnook.ui.users.farmer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.utils.SendPushNotification

class RateDeliveryDialog : DialogFragment() {

    private lateinit var ratingBar: RatingBar
    private lateinit var commentBox: EditText
    private lateinit var rateButton: Button
    private lateinit var closeButton: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_rating, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ratingBar = view.findViewById(R.id.ratingBar)
        commentBox = view.findViewById(R.id.commentBox)
        rateButton = view.findViewById(R.id.rate_button)
        closeButton = view.findViewById(R.id.closeDialog)

        val deliveryId = arguments?.getString("deliveryId")
        val businessId = arguments?.getString("businessId")
        val farmerId = arguments?.getString("farmerId")

        rateButton.setOnClickListener {
            val rating = ratingBar.rating
            val comment = commentBox.text.toString()

            if (rating == 0f) {
                Toast.makeText(context, "Please provide a rating.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (deliveryId.isNullOrEmpty() || businessId.isNullOrEmpty() || farmerId.isNullOrEmpty()) {
                Toast.makeText(context, "Missing data. Cannot submit feedback.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val feedbackRef = db.collection("feedback").document()
            val nowTimestamp = Timestamp.now()

            val feedback = hashMapOf(
                "feedbackId" to feedbackRef.id,
                "deliveryId" to deliveryId,
                "farmerId" to farmerId,
                "businessId" to businessId,
                "rating" to rating,
                "comment" to comment,
                "timestamp" to nowTimestamp
            )

            // Step 1: Submit feedback
            feedbackRef.set(feedback)
                .addOnSuccessListener {
                    val deliveryRef = db.collection("deliveries").document(deliveryId)

                    // Step 2: Set isValidated = true
                    deliveryRef.update("isValidated", true)
                        .addOnSuccessListener {
                            // Step 3: Create deliveryHistory
                            deliveryRef.get().addOnSuccessListener { deliveryDoc ->
                                if (deliveryDoc.exists()) {
                                    val historyId = db.collection("deliveryHistory").document().id
                                    val historyData = hashMapOf(
                                        "historyId" to historyId,
                                        "deliveryId" to deliveryId,
                                        "deliveryArrivalTime" to nowTimestamp,
                                        "remarks" to "N/A"
                                    )

                                    db.collection("deliveryHistory").document(historyId)
                                        .set(historyData)
                                        .addOnSuccessListener {
                                            // Step 4: Send notification
                                            val title = "Delivery Completed"
                                            val message = "A delivery has been completed and rated."

                                            SendPushNotification.sendCompletedDeliveryNotification(
                                                "businessId",
                                                businessId,
                                                deliveryId,
                                                title,
                                                message,
                                                nowTimestamp,
                                                requireContext()
                                            )

                                            Toast.makeText(context, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                                            dismiss()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to create history.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update validation.", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error submitting feedback.", Toast.LENGTH_SHORT).show()
                }
        }

        closeButton.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    companion object {
        fun newInstance(deliveryId: String, businessId: String, farmerId: String): RateDeliveryDialog {
            return RateDeliveryDialog().apply {
                arguments = Bundle().apply {
                    putString("deliveryId", deliveryId)
                    putString("businessId", businessId)
                    putString("farmerId", farmerId)
                }
            }
        }
    }
}

