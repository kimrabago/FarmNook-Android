package com.ucb.capstone.farmnook.ui.farmer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.search.autocomplete.PlaceAutocomplete
import com.mapbox.search.autocomplete.PlaceAutocompleteOptions
import com.mapbox.search.autocomplete.PlaceAutocompleteResult
import com.mapbox.search.autocomplete.PlaceAutocompleteSuggestion
import com.ucb.capstone.farmnook.R
import kotlinx.coroutines.launch

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var locationInput: AutoCompleteTextView
    private lateinit var confirmButton: Button
    private val placeAutocomplete = PlaceAutocomplete.create()
    private var suggestions: List<PlaceAutocompleteSuggestion> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        locationInput = findViewById(R.id.location_input)
        confirmButton = findViewById(R.id.confirm_button)

        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line
        )
        locationInput.setAdapter(adapter)

        // 🔍 Autocomplete search (coroutine-based)
        locationInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.length >= 2) {
                    lifecycleScope.launch {
                        val result = placeAutocomplete.suggestions(query)
                        if (result.isValue) {
                            suggestions = result.value!!
                            val names = suggestions.map { it.name }
                            adapter.clear()
                            adapter.addAll(names)
                            adapter.notifyDataSetChanged()
                        } else {
                            Toast.makeText(
                                this@LocationPickerActivity,
                                "Search failed: ${result.error?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 📌 Handle suggestion selection
        locationInput.setOnItemClickListener { _, _, position, _ ->
            val selectedSuggestion = suggestions.getOrNull(position)
            selectedSuggestion?.let { suggestion ->
                lifecycleScope.launch {
                    val result = placeAutocomplete.select(suggestion)
                    if (result.isValue) {
                        val place: PlaceAutocompleteResult = result.value!!
                        Toast.makeText(
                            this@LocationPickerActivity,
                            "Selected: ${place.name}\nLat: ${place.coordinate.latitude()}, Lng: ${place.coordinate.longitude()}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@LocationPickerActivity,
                            "Error: ${result.error?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // ☑️ Confirm button logic
        confirmButton.setOnClickListener {
            val selected = locationInput.text.toString()
            Toast.makeText(this, "Confirmed location: $selected", Toast.LENGTH_SHORT).show()
        }
    }
}
