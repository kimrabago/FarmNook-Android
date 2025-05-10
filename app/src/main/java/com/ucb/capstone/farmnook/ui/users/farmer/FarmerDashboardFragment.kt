package com.ucb.capstone.farmnook.ui.users.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.menu.NavigationBar
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.ui.users.farmer.add_delivery.AddDeliveryActivity
import com.ucb.capstone.farmnook.utils.loadImage
import com.ucb.capstone.farmnook.viewmodel.users.farmer.FarmerDashboardViewModel

class FarmerDashboardFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var addDeliveryBtn: Button
    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var webViewLoaded = false
    private var profileListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_farmer_dashboard, container, false)

        menuBurger = rootView.findViewById(R.id.menu_burger)
        profileIcon = rootView.findViewById(R.id.profileImage)
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        addDeliveryBtn = rootView.findViewById(R.id.addDeliveryBtn)
        webView = rootView.findViewById(R.id.webView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        profileImageFetch()
        setupWebView()
        setupClickListeners()

        return rootView
    }

    private fun profileImageFetch() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get(Source.CACHE)
                .addOnSuccessListener { document ->
                    val imageUrl = document.getString("profileImageUrl")
                    profileIcon.loadImage(imageUrl)
                }
        }
    }

    private fun setupClickListeners() {
        menuBurger.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        profileIcon.setOnClickListener {
            Handler(Looper.getMainLooper()).post {
                (activity as? NavigationBar)?.navigateToProfile()
            }
        }
        addDeliveryBtn.setOnClickListener {
            val intent = Intent(requireContext(), AddDeliveryActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WEBVIEW", "Page finished loading.")
                webViewLoaded = true

                // Only call if fragment is attached
                if (isAdded && context != null) {
                    checkPermissionsAndStartLocation()
                } else {
                    Log.w("WEBVIEW", "Fragment not attached — skipping location setup.")
                }
            }
        }

        webView.loadUrl("https://farmnook-web.vercel.app/maps?disablePicker=true")
    }

    private fun checkPermissionsAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        if (!isGPSEnabled(requireContext())) {
            Toast.makeText(requireContext(), "Please enable GPS!", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        // Try last known location on resume
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location
                Log.d("LOCATION", "Using last known location: ${location.latitude}, ${location.longitude}")
                sendLocationToWebView(location.latitude, location.longitude)
            } else {
                Log.w("LOCATION", "lastLocation is null, waiting for updates...")
            }
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateDistanceMeters(1f)
            .setMinUpdateIntervalMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    lastLocation = location
                    Log.d("LOCATION_CALLBACK", "Location update: ${location.latitude}, ${location.longitude}")
                    sendLocationToWebView(location.latitude, location.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun sendLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) {
            Log.w("WEBVIEW", "Not sending location — WebView not fully loaded.")
            return
        }

        webView.post {
            val js = "window.updateUserLocation($lat, $lng);"
            Log.d("SEND_LOCATION", "Injecting JS: $js")
            webView.evaluateJavascript(js, null)
        }
    }

    override fun onResume() {
        super.onResume()
        // Always try to re-send last location when user returns
        lastLocation?.let {
            sendLocationToWebView(it.latitude, it.longitude)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        profileListenerRegistration?.remove()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
