package com.ucb.capstone.farmnook.ui.farmer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.search.*
import com.mapbox.search.autocomplete.*
import com.ucb.capstone.farmnook.R

class LocationPickerActivity : AppCompatActivity() {
    private lateinit var locationInput: AutoCompleteTextView

    //    private lateinit var searchEngine: PlaceAutocomplete
    private lateinit var listView: ListView
//    private lateinit var adapter: LocationSuggestionsAdapter
//    private val suggestionsList = mutableListOf<PlaceAutocompleteSuggestion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        locationInput = findViewById(R.id.location_input)
        listView = findViewById(R.id.search_results_list)
        val confirmButton: Button = findViewById(R.id.confirm_button)

//        // Initialize Place Autocomplete with the Mapbox token
//        searchEngine = PlaceAutocomplete.create(getString(R.string.mapbox_access_token))
//
//        // Initialize adapter
//        adapter = LocationSuggestionsAdapter(this, suggestionsList)
//        listView.adapter = adapter
//
//        // Handle text input changes
//        locationInput.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (!s.isNullOrEmpty()) {
//                    fetchLocationSuggestions(s.toString())
//                }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//
//        // Handle list item click
//        listView.setOnItemClickListener { _, _, position, _ ->
//            val selectedSuggestion = suggestionsList[position]
//            locationInput.setText(selectedSuggestion.name)
//        }
//
//        // Confirm selection
//        confirmButton.setOnClickListener {
//            val selectedLocation = locationInput.text.toString()
//            val resultIntent = Intent().apply {
//                putExtra("selected_location", selectedLocation)
//            }
//            setResult(Activity.RESULT_OK, resultIntent)
//            finish()
//        }
//    }
//
//    private fun fetchLocationSuggestions(query: String) {
//        searchEngine.suggestions(query, PlaceAutocompleteOptions()) { result ->
//            result.value?.let { suggestions ->
//                suggestionsList.clear()
//                suggestionsList.addAll(suggestions)
//                adapter.notifyDataSetChanged()
//            }
//        }
//    }
    }
}

