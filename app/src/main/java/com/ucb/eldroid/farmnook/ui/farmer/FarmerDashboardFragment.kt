package com.ucb.eldroid.farmnook.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.locationcomponent.location
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.ui.menu.BottomNavigationBar

class FarmerDashboardFragment : Fragment() {

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var addDeliveryBtn: Button
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true
    private var lastLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_farmer_dashboard, container, false)


        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profileImage)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        addDeliveryBtn = rootView.findViewById(R.id.addDeliveryBtn)
        mapView = rootView.findViewById(R.id.mapView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        profileIcon.setOnClickListener {
            (activity as? BottomNavigationBar)?.navigateToProfile()
        }

        addDeliveryBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddDeliveryActivity::class.java)
            startActivity(intent)
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Request last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { updateMapLocation(it) }
        }

        // 🔹 Define the improved location request with accuracy and filtering
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20000) // 20 seconds interval
            .setMinUpdateDistanceMeters(10f)  // Ignore updates under 10m
            .setMinUpdateIntervalMillis(20000) // 20 seconds interval
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let { location ->
                    updateMapLocation(location)
                }
            }
        }

        // Start requesting location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun updateMapLocation(location: Location) {
        if (lastLocation != null) {
            val distance = location.distanceTo(lastLocation!!).toInt()
            val speed = location.speed
            val timeDifference = (location.time - lastLocation!!.time) / 1000 // Time in seconds

            // Ignore updates if distance < 10m, speed < 0.5m/s, or time difference < 5s
            if (distance < 10 || speed < 0.5 || timeDifference < 5) {
                return
            }
        }

        lastLocation = location // Update stored location

        val userLocation = Point.fromLngLat(location.longitude, location.latitude)

        // Preserve zoom level if already set
        val zoomLevel = if (isFirstLocationUpdate) {
            isFirstLocationUpdate = false
            20.0 // Initial zoom level
        } else {
            mapView.getMapboxMap().cameraState.zoom // Preserve user zoom
        }

        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(userLocation)
                .zoom(zoomLevel)
                .build()
        )

        // Enable real-time location tracking
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
