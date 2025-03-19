package com.ucb.eldroid.farmnook.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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

        // Initialize views
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
        checkPermissionsAndSetupLocation()
    }

    private fun checkPermissionsAndSetupLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            setupLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        // Ensure GPS is enabled
        if (!isGPSEnabled()) {
            Toast.makeText(requireContext(), "Please enable GPS!", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        // Request last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { updateMapLocation(it) }
        }

        // Define location request settings
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20000) // 20 sec interval
            .setMinUpdateDistanceMeters(10f)  // Minimum distance of 10m before update
            .setMinUpdateIntervalMillis(20000) // 20 sec interval
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

    private fun isGPSEnabled(): Boolean {
        val locationMode: Int = try {
            Settings.Secure.getInt(requireContext().contentResolver, Settings.Secure.LOCATION_MODE)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    private fun updateMapLocation(location: Location) {
        // Ensure valid location
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Log.e("LocationUpdate", "Invalid GPS location received")
            return
        }

        // Avoid unnecessary updates
        if (lastLocation != null) {
            val distance = location.distanceTo(lastLocation!!).toInt()
            val speed = location.speed
            val timeDifference = (location.time - lastLocation!!.time) / 1000

            if (distance < 10 || speed < 0.5 || timeDifference < 5) {
                return // Ignore small fluctuations
            }
        }

        lastLocation = location // Store last known location

        val userLocation = Point.fromLngLat(location.longitude, location.latitude)

        // Set initial zoom level
        val zoomLevel = if (isFirstLocationUpdate) {
            isFirstLocationUpdate = false
            20.0 // Set initial zoom to 15
        } else {
            mapView.getMapboxMap().cameraState.zoom // Maintain user zoom level
        }

        // Move camera to user location
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(userLocation)
                .zoom(zoomLevel)
                .build()
        )

        // Enable location tracking on Mapbox
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }

        Log.d("LocationUpdate", "Updated location: Lat: ${location.latitude}, Lng: ${location.longitude}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
