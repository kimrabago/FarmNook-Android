package com.ucb.capstone.farmnook.ui.hauler

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.adapter.PlaceAutocompleteAdapter
import java.util.Locale

class LocationPickerActivity : AppCompatActivity() {

    private var selectedCoordinates: String? = null
    private var fullAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        // Initialize Places API
        Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.ENGLISH)

        val locationInput = findViewById<AutoCompleteTextView>(R.id.location_input)
        val adapter = PlaceAutocompleteAdapter(this, Locale("fil", "PH"))
        locationInput.setAdapter(adapter)

        // Handle place selection
        locationInput.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as AutocompletePrediction
            val placeId = selected.placeId

            val placeFields = listOf(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.LAT_LNG
            )
            val request = FetchPlaceRequest.builder(placeId, placeFields).build()
            val placesClient = Places.createClient(this)

            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                val addressComponents = place.addressComponents
                val addressParts = LinkedHashSet<String>()

                addressComponents?.asList()?.forEach { component ->
                    val types = component.types
                    val name = component.name

                    // Skip "Central Visayas"
                    if (name.equals("Central Visayas", ignoreCase = true)) return@forEach

                    if (types.contains("premise") || types.contains("route") || types.contains("street_address")) {
                        addressParts.add(name)
                    }
                    if (types.contains("sublocality") || types.contains("sublocality_level_1")) {
                        addressParts.add(name)
                    }
                    if (types.contains("locality")) {
                        addressParts.add(name)
                    }
                    if (types.contains("administrative_area_level_1")) {
                        addressParts.add(name)
                    }
                }

                fullAddress = buildString {
                    place.name?.let { append(it) }
                    if (addressParts.isNotEmpty()) {
                        if (isNotEmpty()) append(", ")
                        append(addressParts.joinToString(", "))
                    }
                }

                locationInput.setText(fullAddress)

                val latLng = place.latLng
                selectedCoordinates = if (latLng != null) "${latLng.latitude},${latLng.longitude}" else null
            }
        }

        // Confirm button
        findViewById<Button>(R.id.confirm_button).setOnClickListener {
            if (selectedCoordinates.isNullOrEmpty() || fullAddress.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a valid location from the list.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putExtra("selectedLocation", fullAddress)
                putExtra("selectedCoordinates", selectedCoordinates)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // Filter suggestions as user types
        locationInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }
        })
    }
}
