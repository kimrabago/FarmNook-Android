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

            val feedback = hashMapOf(
                "feedbackId" to feedbackRef.id,
                "deliveryId" to deliveryId,
                "farmerId" to farmerId,
                "businessId" to businessId,
                "rating" to rating,
                "comment" to comment,
                "timestamp" to Timestamp.now()
            )

            feedbackRef.set(feedback)
                .addOnSuccessListener {
                    Toast.makeText(context, "Feedback submitted!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error submitting feedback.", Toast.LENGTH_SHORT).show()
                }
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    companion object {
        fun newInstance(deliveryId: String, businessId: String, farmerId: String): RateDeliveryDialog {
            val dialog = RateDeliveryDialog()
            val args = Bundle()
            args.putString("deliveryId", deliveryId)
            args.putString("businessId", businessId)
            args.putString("farmerId", farmerId)
            dialog.arguments = args
            return dialog
        }
    }
}
