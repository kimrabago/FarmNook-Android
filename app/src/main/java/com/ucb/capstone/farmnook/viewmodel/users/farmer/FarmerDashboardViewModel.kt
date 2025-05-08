package com.ucb.capstone.farmnook.viewmodel.users.farmer

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class FarmerDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _location = MutableLiveData<Location?>()
    val location: LiveData<Location?> = _location

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    fun fetchProfileImage() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get(Source.CACHE)
            .addOnSuccessListener { document ->
                _profileImageUrl.postValue(document.getString("profileImageUrl"))
            }
    }

    fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateDistanceMeters(1f)
            .setMinUpdateIntervalMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { _location.postValue(it) }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (_: SecurityException) {}
    }

    fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { _location.postValue(it) }
        } catch (_: SecurityException) {}
    }
}