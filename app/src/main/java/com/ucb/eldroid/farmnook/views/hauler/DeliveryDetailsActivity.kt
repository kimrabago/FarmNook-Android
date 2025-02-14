package com.ucb.eldroid.farmnook.views.hauler

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.common.location.Location
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.gestures.gestures
import com.ucb.eldroid.farmnook.R

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationComponentPlugin: LocationComponentPlugin
    private lateinit var gesturesPlugin: GesturesPlugin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_details)

        mapView = findViewById(R.id.mapView)

        // Load Mapbox style and setup plugins
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->

            // Initialize gestures plugin
//            gesturesPlugin = mapView.gestures
//
//            // Enable gestures
//            gesturesPlugin.setRotateEnabled(true)  // Enable rotation
//            gesturesPlugin.setZoomEnabled(true)    // Enable zoom
//            gesturesPlugin.setScrollEnabled(true) // Enable scroll
//
//            // Initialize location component plugin
//            locationComponentPlugin = mapView.locationComponent
//
//            // Enable location component
//            locationComponentPlugin.apply {
//                isLocationComponentEnabled = true
//            }
//
//            // Add location listener for location updates
//            locationComponentPlugin.addOnIndicatorPositionChangedListener(object : OnIndicatorPositionChangedListener {
//                override fun onIndicatorPositionChanged(location: Location) {
//                    // When location is updated, center the map on the user's location
//                    mapView.getMapboxMap().setCamera(
//                        CameraOptions.Builder()
//                            .center(location.coordinate) // Update with new location
//                            .zoom(14.0) // Adjust zoom level as needed
//                            .build()
//                    )
//                }
//            })
//
//            // Add a scale bar
//            mapView.getMapboxMap().scalebar.enabled = true
//        }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
