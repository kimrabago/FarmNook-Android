package com.ucb.capstone.farmnook.ui.users

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private lateinit var webView: WebView
    private var webViewLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        Places.initialize(applicationContext, getString(R.string.google_maps_key), Locale.ENGLISH)

        val locationInput = findViewById<AutoCompleteTextView>(R.id.location_input)
        val adapter = PlaceAutocompleteAdapter(this, Locale("fil", "PH"))
        locationInput.setAdapter(adapter)

        locationInput.setOnItemClickListener { parent, _, position, _ ->
            locationInput.setText("")

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

                locationInput.post {
                    locationInput.setText(fullAddress)
                    locationInput.setSelection(fullAddress?.length ?: 0)
                    locationInput.dismissDropDown()
                }

                val latLng = place.latLng
                selectedCoordinates = if (latLng != null) {
                    val lat = latLng.latitude
                    val lng = latLng.longitude
                    if (webViewLoaded) {
                        sendLocationToWebView(lat, lng)
                    }
                    "$lat,$lng"
                } else null
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

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webViewLoaded = true

                selectedCoordinates?.let {
                    val parts = it.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) {
                            sendLocationToWebView(lat, lng)
                        }
                    }
                }
            }
        }

        webView.loadUrl("https://farmnook-web.vercel.app/maps?disablePicker=true")
    }

    private fun sendLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) return

        webView.post {
            val js = "window.updateUserLocation($lat, $lng);"
            webView.evaluateJavascript(js, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupWebView()
    }

    private fun cleanupWebView() {
        try {
            webView.apply {
                loadUrl("https://farmnook-web.vercel.app/maps?disablePicker=true")
                clearHistory()
                removeAllViews()
                destroy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
