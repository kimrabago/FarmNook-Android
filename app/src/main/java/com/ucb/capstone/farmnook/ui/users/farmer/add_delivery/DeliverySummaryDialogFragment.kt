import android.app.Dialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import com.ucb.capstone.farmnook.ui.users.farmer.add_delivery.ScheduleBottomSheet
import java.util.Locale
import com.ucb.capstone.farmnook.util.getAddressFromLatLng
import com.ucb.capstone.farmnook.utils.CombineTimeDurations
import com.ucb.capstone.farmnook.utils.loadImage
import java.util.Calendar

class DeliverySummaryDialogFragment : DialogFragment() {

    private lateinit var vehicleWtBusiness: VehicleWithBusiness
    private lateinit var deliveryReq: DeliveryRequest
    private var onHireConfirmed: ((Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit)? = null

    companion object {
        fun newInstance(
            vehicleWithBusiness: VehicleWithBusiness,
            deliveryRequest: DeliveryRequest,
            onHire: (Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit
        ): DeliverySummaryDialogFragment {
            val fragment = DeliverySummaryDialogFragment()
            fragment.vehicleWtBusiness = vehicleWithBusiness
            fragment.deliveryReq = deliveryRequest
            fragment.onHireConfirmed = onHire
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_delivery_summary, null)

        view.findViewById<TextView>(R.id.businessName).text = vehicleWtBusiness.businessName
        val profileImageUrl = vehicleWtBusiness.profileImage
        val profileImage = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
        profileImage.loadImage(profileImageUrl)

        view.findViewById<TextView>(R.id.businessName).text = vehicleWtBusiness.businessName
        view.findViewById<TextView>(R.id.businessLocation).text = vehicleWtBusiness.locationName

        // Set values in included item_detail_row views
        setItemRow(view, R.id.plateRow, "Plate Number", vehicleWtBusiness.plateNumber)
        deliveryReq.pickupName?.let { setItemRow(view, R.id.pickupRow, "Pickup", it) }
        deliveryReq.destinationName?.let {
            setItemRow(view, R.id.destinationRow, "Destination",
                it
            )
        }
        setItemRow(view, R.id.vehicleRow, "Vehicle", "${vehicleWtBusiness.vehicleType} - ${vehicleWtBusiness.model}")
        setItemRow(view, R.id.purposeRow, "Purpose",
            deliveryReq.purpose?.replaceFirstChar { it.uppercase() } ?: "")
        setItemRow(view, R.id.productRow, "Product", "${deliveryReq.productType} (${deliveryReq.weight} kg)")
        setItemRow(view, R.id.weightRow, "Receiver's Info", "${deliveryReq.receiverName} - ${deliveryReq.receiverNumber} ")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()


        view.findViewById<View>(R.id.hireButton).setOnClickListener {
            view.post {
                val prepMinutes = 30
                val vehicleId = vehicleWtBusiness.vehicleId
                Log.d("DEBUG_VEHICLE_ID", "Fetching booked times for vehicleId: $vehicleId")

                FirebaseFirestore.getInstance()
                    .collection("deliveries")
                    .whereEqualTo("vehicleId", vehicleId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val bookedTimes =
                            snapshot.documents.mapNotNull { it.getTimestamp("scheduledTime") }

                        val bottomSheet = ScheduleBottomSheet(
                            prepMinutes = prepMinutes,
                            bookedTimes = bookedTimes,
                            vehicleId = vehicleId,
                            overallEstMinutes = deliveryReq.overallEstimatedTime ?: 0
                        ) { selectedTimestamp ->
                            val updatedDeliveryReq = deliveryReq.copy(
                                scheduledTime = selectedTimestamp,
                                estimatedEndTime = calculateEstimatedEndTime(
                                    selectedTimestamp,
                                    deliveryReq.overallEstimatedTime
                                )
                            )
                            onHireConfirmed?.invoke(vehicleWtBusiness to updatedDeliveryReq)
                            dismiss()
                        }

                        bottomSheet.show(parentFragmentManager, "ScheduleBottomSheet")
                    }
            }
        }

        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setItemRow(parent: View, rowId: Int, label: String, value: String) {
        val row = parent.findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.label).text = label
        row.findViewById<TextView>(R.id.value).text = value
    }

    private fun calculateEstimatedEndTime(startTime: Timestamp, overallMinutes: Int?): Timestamp {
        val startDate = startTime.toDate()
        val cal = Calendar.getInstance().apply {
            time = startDate
            add(Calendar.MINUTE, overallMinutes ?: 0)

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
        return Timestamp(cal.time)
    }
}
