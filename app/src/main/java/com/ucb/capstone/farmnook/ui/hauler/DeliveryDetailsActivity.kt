package com.ucb.capstone.farmnook.ui.hauler

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.ucb.capstone.farmnook.R

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        mapView = findViewById(R.id.mapView)

        // Load Mapbox style and setup plugins
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->

            val backButton = findViewById<ImageButton>(R.id.btn_back)
            backButton.setOnClickListener {
                finish()
            }

            val bottomSheet = findViewById<View>(R.id.bottomSheet)
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.peekHeight = 200
            bottomSheetBehavior.isHideable = false
        }
    }
}
