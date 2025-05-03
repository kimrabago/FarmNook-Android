import android.app.Dialog
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import java.util.Locale
import com.ucb.capstone.farmnook.util.getAddressFromLatLng
import com.ucb.capstone.farmnook.utils.loadImage

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

        // Convert lat/lng to readable addresses
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val pickupAddress = getAddressFromLatLng(deliveryReq.pickupLocation, geocoder)
        val destinationAddress = getAddressFromLatLng(deliveryReq.destinationLocation, geocoder)
        val businessLoc = getAddressFromLatLng(vehicleWtBusiness.businessLocation, geocoder)
        view.findViewById<TextView>(R.id.businessName).text = vehicleWtBusiness.businessName
        val profileImageUrl = vehicleWtBusiness.profileImage
        val profileImage = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
        profileImage.loadImage(profileImageUrl)

        view.findViewById<TextView>(R.id.businessName).text = vehicleWtBusiness.businessName
        view.findViewById<TextView>(R.id.businessLocation).text = businessLoc

        // Set values in included item_detail_row views
        setItemRow(view, R.id.plateRow, "Plate Number", vehicleWtBusiness.plateNumber)
        setItemRow(view, R.id.pickupRow, "Pickup", pickupAddress)
        setItemRow(view, R.id.destinationRow, "Destination", destinationAddress)
        setItemRow(view, R.id.vehicleRow, "Vehicle", "${vehicleWtBusiness.vehicleType} - ${vehicleWtBusiness.model}")
        setItemRow(view, R.id.purposeRow, "Purpose",
            deliveryReq.purpose?.replaceFirstChar { it.uppercase() } ?: "")
        setItemRow(view, R.id.productRow, "Product Type", "${deliveryReq.productType}")
        setItemRow(view, R.id.weightRow, "Weight", "${deliveryReq.weight} kg")


        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<View>(R.id.hireButton).setOnClickListener {
            // Invoke the callback (onHireConfirmed) to perform any additional logic
            onHireConfirmed?.invoke(vehicleWtBusiness to deliveryReq)
            dismiss()
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
}
