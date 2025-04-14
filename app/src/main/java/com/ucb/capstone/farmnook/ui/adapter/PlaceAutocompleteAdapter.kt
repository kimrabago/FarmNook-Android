package com.ucb.capstone.farmnook.ui.adapter

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.tasks.await
import java.util.Locale

class PlaceAutocompleteAdapter(
    context: Context,
    private val locale: Locale = Locale.ENGLISH
) : ArrayAdapter<AutocompletePrediction>(context, android.R.layout.simple_list_item_1) {

    private val placesClient = Places.createClient(context)
    private val predictions = mutableListOf<AutocompletePrediction>()

    override fun getCount(): Int = predictions.size
    override fun getItem(position: Int): AutocompletePrediction? = predictions[position]
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = super.getView(position, convertView, parent)
        itemView.findViewById<TextView>(android.R.id.text1).text = predictions[position].getFullText(null)
        return itemView
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults() // Don't filter here
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (constraint == null || constraint.isEmpty()) return


                val cebuBounds = RectangularBounds.newInstance(
                    LatLng(9.3719, 123.9950), // Southwest
                    LatLng(11.3720, 124.1830) // Northeast
                )

                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(constraint.toString())
                    .setLocationBias(cebuBounds)
                    .setCountries(listOf("PH"))
                    .build()

                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response ->
                        predictions.clear()
                        predictions.addAll(
                            response.autocompletePredictions.filter {
                                it.getFullText(null).toString().contains("Cebu", ignoreCase = true)
                            }
                        )
                        (context as? Activity)?.runOnUiThread {
                            notifyDataSetChanged()
                        }

                        Log.d("PlacesDebug", "Filtered predictions: ${predictions.size}")
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
                    }
            }
        }
    }
}
