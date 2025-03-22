package com.ucb.capstone.farmnook.ui.farmer

import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.Button
//import com.mapbox.search.SearchEngine
//import com.mapbox.search.SearchEngineSettings
//import com.mapbox.search.result.SearchSuggestion
//import com.mapbox.search.ResponseInfo
//import com.mapbox.search.SearchOptions
//import com.mapbox.search.SearchCallback
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R

class LocationPickerActivity : AppCompatActivity() {
//    private lateinit var locationInput: AutoCompleteTextView
//    private lateinit var searchEngine: SearchEngine
//    private var locationType: String? = null
//    private val suggestionList = ArrayList<String>()
//    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)


//        locationInput = findViewById(R.id.location_input)
//        val confirmButton: Button = findViewById(R.id.confirm_button)
//
//        locationType = intent.getStringExtra("location_type")
//
//        // Initialize Mapbox Search API
//        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
//            SearchEngineSettings("YOUR_MAPBOX_ACCESS_TOKEN") // Replace with your API key
//        )
//
//        // Set up the adapter for location suggestions
//        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggestionList)
//        locationInput.setAdapter(adapter)
//
//        // Add text change listener to fetch location suggestions
//        locationInput.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {}
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                if (s.toString().isNotEmpty()) {
//                    fetchLocationSuggestions(s.toString())
//                }
//            }
//        })
//
//        confirmButton.setOnClickListener {
//            val selectedLocation = locationInput.text.toString()
//            val resultIntent = Intent().apply {
//                putExtra("location_type", locationType)
//                putExtra("selected_location", selectedLocation)
//            }
//            setResult(Activity.RESULT_OK, resultIntent)
//            finish()
//        }
//    }
//
//    private fun fetchLocationSuggestions(query: String) {
//        val searchOptions = SearchOptions.Builder().limit(5).build()
//
//        searchEngine.search(query, searchOptions, object : SearchCallback {
//            override fun onResults(results: List<SearchSuggestion>, responseInfo: ResponseInfo) {
//                suggestionList.clear()
//                results.forEach { suggestion ->
//                    suggestionList.add(suggestion.name)
//                }
//                adapter.notifyDataSetChanged()
//            }
//
//            override fun onError(e: Exception) {
//                e.printStackTrace()
//            }
//        })
      }
}
