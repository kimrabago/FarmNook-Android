    package com.ucb.capstone.farmnook.ui.farmer.add_delivery

    import android.os.Bundle
    import android.util.Log
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
        private lateinit var closeButton: ImageView  // Reference to close button

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.rating_dialog, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            ratingBar = view.findViewById(R.id.ratingBar)
            commentBox = view.findViewById(R.id.commentBox)
            rateButton = view.findViewById(R.id.rate_button)
            closeButton = view.findViewById(R.id.closeDialog)  // Initialize close button

            // Get data from arguments passed to the dialog
            val deliveryId = arguments?.getString("deliveryId")
            val businessId = arguments?.getString("businessId")
            val farmerId = arguments?.getString("farmerId")

            // Set up click listener for the rate button
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
                val feedbackRef = db.collection("feedback").document() // Auto-generate doc ID

                val feedback = hashMapOf(
                    "feedbackId" to feedbackRef.id, // Match doc ID
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
                    .addOnFailureListener { e ->
                        Log.e("RateDelivery", "Failed to submit feedback", e)
                        Toast.makeText(context, "Error submitting feedback.", Toast.LENGTH_SHORT).show()
                    }
            }

            // Set up click listener for the close button
            closeButton.setOnClickListener {
                dismiss()  // Close the dialog when the close button is clicked
            }
        }

        // Function to close the dialog (could be used in other places if needed)
        private fun closeDialog() {
            dismiss()  // Close the dialog
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
