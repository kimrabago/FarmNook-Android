package com.ucb.capstone.farmnook.ui.hauler

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.*
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class HaulerDeliveryStatusFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastLocation: Location? = null
    private var webViewLoaded = false
    private var mapUrl: String? = null
    private var mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hauler_delivery_status, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize WebView first
        webView = view.findViewById(R.id.mapView)
        setupWebView()

        // Initialize other UI components
        val fromAddress = view.findViewById<TextView>(R.id.from_address)
        val toAddress = view.findViewById<TextView>(R.id.to_address)
        val totalKilometer = view.findViewById<TextView>(R.id.totalKilometer)
        val durationTime = view.findViewById<TextView>(R.id.durationTime)
        val status = view.findViewById<TextView>(R.id.status)

        // Initialize bottom sheet behavior
        val bottomSheet = view.findViewById<View>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED

        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Get deliveryId from arguments or fetch active delivery
        val deliveryId = arguments?.getString("deliveryId")
        if (deliveryId != null) {
            // Case 1: Coming from start button with deliveryId
            fetchDeliveryData(deliveryId, fromAddress, toAddress, totalKilometer, durationTime, status)
        } else {
            // Case 2: Direct navigation, fetch active delivery
            fetchActiveDelivery(fromAddress, toAddress, totalKilometer, durationTime, status)
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebViewConsole", "JS: ${consoleMessage?.message()}")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebViewStatus", "✅ WebView finished loading: $url")
                webViewLoaded = true
                checkPermissionsAndStartLocation()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e("WebViewError", "❌ WebView error: ${error?.description}")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }
    }

    private fun fetchActiveDelivery(
        fromAddress: TextView,
        toAddress: TextView,
        totalKilometer: TextView,
        durationTime: TextView,
        status: TextView
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("deliveries")
            .whereEqualTo("haulerAssignedId", currentUserId)
            .whereEqualTo("isStarted", true)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No active delivery found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val deliveryDoc = querySnapshot.documents.first()
                val deliveryId = deliveryDoc.id
                fetchDeliveryData(deliveryId, fromAddress, toAddress, totalKilometer, durationTime, status)
            }
            .addOnFailureListener { e ->
                Log.e("DeliveryStatus", "❌ Failed to fetch active delivery", e)
                Toast.makeText(requireContext(), "Failed to load active delivery", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchDeliveryData(
        deliveryId: String,
        fromAddress: TextView,
        toAddress: TextView,
        totalKilometer: TextView,
        durationTime: TextView,
        status: TextView
    ) {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("deliveries").document(deliveryId)
            .get()
            .addOnSuccessListener { deliveryDoc ->
                if (!deliveryDoc.exists()) {
                    Toast.makeText(requireContext(), "Delivery not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                val haulerId = deliveryDoc.getString("haulerAssignedId") ?: "anonymous"
                val isArrivedAtPickup = deliveryDoc.getBoolean("arrivedAtPickup") ?: false
                val isArrivedAtDestination = deliveryDoc.getBoolean("arrivedAtDestination") ?: false
                val isDone = deliveryDoc.getBoolean("isDone") ?: false

                val statusText = when {
                    isDone -> "Completed"
                    isArrivedAtDestination -> "Arrived at Destination"
                    isArrivedAtPickup -> "On the Way"
                    else -> "Going to Pickup"
                }
                status.text = statusText

                firestore.collection("deliveryRequests").document(requestId)
                    .get()
                    .addOnSuccessListener { requestDoc ->
                        val pickup = requestDoc.getString("pickupLocation") ?: return@addOnSuccessListener
                        val drop = requestDoc.getString("destinationLocation") ?: return@addOnSuccessListener
                        
                        // Get the addresses using reverse geocoding
                        reverseGeocode(pickup) { pickupAddress ->
                            reverseGeocode(drop) { dropAddress ->
                                // Calculate distance and time
                                getEstimatedTravelTime(pickup, drop) { estimatedTime, distance ->
                                    // Update UI with the delivery data
                                    fromAddress.text = pickupAddress
                                    toAddress.text = dropAddress
                                    totalKilometer.text = "${String.format("%.1f", distance)} km"
                                    durationTime.text = estimatedTime

                                    // Load the map with the correct locations
                                    mapUrl = "https://farmnook-web.vercel.app/live-tracking?pickup=${pickup.replace(" ", "")}&drop=${drop.replace(" ", "")}&haulerId=$haulerId"
                                    Log.d("WebViewStatus", "🌐 Map URL: $mapUrl")
                                    webView.loadUrl(mapUrl!!)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DeliveryStatus", "❌ Failed to get delivery request document", e)
                        Toast.makeText(requireContext(), "Failed to load delivery details", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DeliveryStatus", "❌ Failed to fetch delivery data from Firestore", e)
                Toast.makeText(requireContext(), "Failed to load delivery", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reverseGeocode(latLng: String, callback: (String) -> Unit) {
        val (lat, lng) = latLng.split(",").map { it.trim().toDouble() }
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=$mapboxToken"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Unknown location")
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    val address = try {
                        val json = JSONObject(body ?: "")
                        json.getJSONArray("features")
                            .optJSONObject(0)
                            ?.getString("place_name") ?: "Unknown location"
                    } catch (e: Exception) {
                        "Unknown location"
                    }
                    Handler(Looper.getMainLooper()).post { callback(address) }
                }
            })
    }

    private fun getEstimatedTravelTime(pickup: String, drop: String, callback: (String, Double) -> Unit) {
        val (startLat, startLng) = pickup.split(",").map { it.trim() }
        val (endLat, endLng) = drop.split(",").map { it.trim() }

        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$startLng,$startLat;$endLng,$endLat?access_token=$mapboxToken&overview=false"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Unknown", 0.0)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "")
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val durationSec = route.getDouble("duration")
                        val distanceKm = route.getDouble("distance") / 1000.0
                        
                        val minutes = (durationSec / 60).toInt()
                        val estimated = if (minutes < 60) "$minutes min"
                        else "${minutes / 60} hr ${minutes % 60} min"
                        
                        Handler(Looper.getMainLooper()).post { callback(estimated, distanceKm) }
                    } else {
                        callback("Unknown", 0.0)
                    }
                } catch (e: Exception) {
                    callback("Unknown", 0.0)
                }
            }
        })
    }

    private fun checkPermissionsAndStartLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            setupLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                lastLocation = it
                injectLocationToWebView(it.latitude, it.longitude)
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateDistanceMeters(1f)
            .setMinUpdateIntervalMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    lastLocation = it
                    injectLocationToWebView(it.latitude, it.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun injectLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) {
            Log.w("SEND_LOCATION", "⚠️ WebView not ready — cannot inject location yet.")
            return
        }
        
        // Update Firebase Realtime Database
        val firebase = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.uid?.let { userId ->
            firebase.collection("haulerLocations").document(userId)
                .set(mapOf(
                    "latitude" to lat,
                    "longitude" to lng,
                    "timestamp" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    Log.d("SEND_LOCATION", "✅ Location updated in Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e("SEND_LOCATION", "❌ Failed to update location in Firebase", e)
                }
        }

        // Inject location to WebView
        val js = """
            if (window.updateUserLocation) {
                window.updateUserLocation($lat, $lng);
            } else {
                console.error('updateUserLocation function not found');
            }
        """.trimIndent()
        
        Log.d("SEND_LOCATION", "📡 Injecting JS: $js")
        webView.post {
            webView.evaluateJavascript(js) { result ->
                Log.d("SEND_LOCATION", "JS Evaluation Result: $result")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

