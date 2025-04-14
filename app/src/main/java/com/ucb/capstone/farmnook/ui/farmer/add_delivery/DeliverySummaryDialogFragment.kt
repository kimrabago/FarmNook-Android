import android.app.Dialog
import android.location.Geocoder
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.VehicleWithBusiness
import java.util.Locale

class DeliverySummaryDialogFragment : DialogFragment() {

    private lateinit var vehicle: VehicleWithBusiness
    private lateinit var delivery: DeliveryRequest
    private var onHireConfirmed: ((Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit)? = null

    companion object {
        fun newInstance(
            vehicle: VehicleWithBusiness,
            delivery: DeliveryRequest,
            onHire: (Pair<VehicleWithBusiness, DeliveryRequest>) -> Unit
        ): DeliverySummaryDialogFragment {
            val fragment = DeliverySummaryDialogFragment()
            fragment.vehicle = vehicle
            fragment.delivery = delivery
            fragment.onHireConfirmed = onHire
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val pickupAddress = getAddressFromLatLng(delivery.pickupLocation, geocoder)
        val destinationAddress = getAddressFromLatLng(delivery.destinationLocation, geocoder)

        return AlertDialog.Builder(requireContext())
            .setTitle("Confirm Hire")
            .setMessage(
                """
                Business: ${vehicle.businessName}
                Vehicle: ${vehicle.vehicleType} - ${vehicle.model}
                Plate Number: ${vehicle.plateNumber}
                
                Pickup: $pickupAddress
                Destination: $destinationAddress
                Purpose: ${delivery.purpose}
                Product Type: ${delivery.productType}
                Weight: ${delivery.weight} kg
                
                Proceed with delivery request?
                """.trimIndent()
            )
            .setPositiveButton("Hire") { _, _ ->
                onHireConfirmed?.invoke(vehicle to delivery)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun getAddressFromLatLng(locationStr: String?, geocoder: Geocoder): String {
        return try {
            if (!locationStr.isNullOrBlank()) {
                val parts = locationStr.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].toDouble()
                    val lng = parts[1].toDouble()
                    val addressList = geocoder.getFromLocation(lat, lng, 1)
                    if (!addressList.isNullOrEmpty()) {
                        addressList[0].getAddressLine(0)
                    } else {
                        "Unknown location"
                    }
                } else {
                    "Invalid coordinates"
                }
            } else {
                "Not specified"
            }
        } catch (e: Exception) {
            "Error resolving address"
        }
    }
}
