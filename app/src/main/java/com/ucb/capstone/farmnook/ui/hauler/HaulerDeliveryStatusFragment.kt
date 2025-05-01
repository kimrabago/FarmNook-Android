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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HaulerDeliveryStatusFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var webViewLoaded = false
    private var lastLocation: Location? = null
    private var deliveryId: String? = null
    private var pickupCoords: String? = null
    private var dropCoords: String? = null
    private var currentStatus = "Going to Pickup"
    private var haulerId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val firestore = FirebaseFirestore.getInstance()
    private val mapboxToken = "pk.eyJ1Ijoia2ltcmFiYWdvIiwiYSI6ImNtNnRjbm94YjAxbHAyaXNoamk4aThldnkifQ.OSRIDYIw-6ff3RNJVYwspg"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hauler_delivery_status, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.mapView)
        setupWebView()

        val fromAddress = view.findViewById<TextView>(R.id.from_address)
        val toAddress = view.findViewById<TextView>(R.id.to_address)
        val totalKilometer = view.findViewById<TextView>(R.id.totalKilometer)
        val durationTime = view.findViewById<TextView>(R.id.durationTime)
        val status = view.findViewById<TextView>(R.id.status)

        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottomSheet)).apply {
            peekHeight = 100
            isHideable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Get deliveryId
        deliveryId = arguments?.getString("deliveryId")
        if (deliveryId != null) {
            fetchDeliveryData(deliveryId!!, fromAddress, toAddress, totalKilometer, durationTime, status)
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
                webViewLoaded = true
                checkPermissionsAndStartLocation()
            }
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
        firestore.collection("deliveries").document(deliveryId)
            .get()
            .addOnSuccessListener { deliveryDoc ->
                val requestId = deliveryDoc.getString("requestId") ?: return@addOnSuccessListener
                haulerId = deliveryDoc.getString("haulerAssignedId") ?: haulerId
                val isArrivedAtPickup = deliveryDoc.getBoolean("arrivedAtPickup") ?: false
                val isArrivedAtDestination = deliveryDoc.getBoolean("arrivedAtDestination") ?: false
                val isDone = deliveryDoc.getBoolean("isDone") ?: false

                currentStatus = when {
                    isDone -> "Completed"
                    isArrivedAtDestination -> "Arrived at Destination"
                    isArrivedAtPickup -> "On the Way"
                    else -> "Going to Pickup"
                }
                status.text = currentStatus

                firestore.collection("deliveryRequests").document(requestId)
                    .get()
                    .addOnSuccessListener { req ->
                        pickupCoords = req.getString("pickupLocation") ?: return@addOnSuccessListener
                        dropCoords = req.getString("destinationLocation") ?: return@addOnSuccessListener

                        reverseGeocode(pickupCoords!!) { pickupAddr ->
                            reverseGeocode(dropCoords!!) { dropAddr ->
                                getEstimatedTravelTime(pickupCoords!!, dropCoords!!) { eta, km ->
                                    fromAddress.text = pickupAddr
                                    toAddress.text = dropAddr
                                    durationTime.text = eta
                                    totalKilometer.text = "${String.format("%.1f", km)} km"

                                    val mapUrl = "https://farmnook-web.vercel.app/live-tracking?pickup=${pickupCoords!!.replace(" ", "")}&drop=${dropCoords!!.replace(" ", "")}&haulerId=$haulerId"
                                    webView.loadUrl(mapUrl)
                                }
                            }
                        }
                    }
            }
    }

    private fun reverseGeocode(latLng: String, callback: (String) -> Unit) {
        val (lat, lng) = latLng.split(",").map { it.trim() }
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=$mapboxToken"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Unknown")
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.body?.string()
                    val address = try {
                        val json = JSONObject(result ?: "")
                        json.getJSONArray("features").optJSONObject(0)?.getString("place_name") ?: "Unknown"
                    } catch (e: Exception) {
                        "Unknown"
                    }
                    Handler(Looper.getMainLooper()).post { callback(address) }
                }
            })
    }

    private fun getEstimatedTravelTime(pickup: String, drop: String, callback: (String, Double) -> Unit) {
        val (startLat, startLng) = pickup.split(",")
        val (endLat, endLng) = drop.split(",")

        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$startLng,$startLat;$endLng,$endLat?access_token=$mapboxToken&overview=false"

        OkHttpClient().newCall(Request.Builder().url(url).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback("Unknown", 0.0)
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val route = json.getJSONArray("routes").optJSONObject(0)
                    val duration = route?.getDouble("duration") ?: 0.0
                    val distance = route?.getDouble("distance") ?: 0.0

                    val mins = (duration / 60).toInt()
                    val eta = if (mins < 60) "$mins min" else "${mins / 60} hr ${mins % 60} min"
                    Handler(Looper.getMainLooper()).post { callback(eta, distance / 1000) }
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLocation = location
                injectLocationToWebView(location.latitude, location.longitude)
                deliveryId?.let { checkProximityAndUpdate(it, location) }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun checkProximityAndUpdate(deliveryId: String, location: Location) {
        val deliveryRef = firestore.collection("deliveries").document(deliveryId)

        if (pickupCoords == null || dropCoords == null) return

        val (pickupLat, pickupLng) = pickupCoords!!.split(",").map { it.toDouble() }
        val (dropLat, dropLng) = dropCoords!!.split(",").map { it.toDouble() }

        val pickupLoc = Location("").apply { latitude = pickupLat; longitude = pickupLng }
        val dropLoc = Location("").apply { latitude = dropLat; longitude = dropLng }

        deliveryRef.get().addOnSuccessListener { doc ->
            val atPickup = doc.getBoolean("arrivedAtPickup") ?: false
            val atDestination = doc.getBoolean("arrivedAtDestination") ?: false

            when {
                !atPickup && location.distanceTo(pickupLoc) <= 20 -> {
                    deliveryRef.update("arrivedAtPickup", true)
                    currentStatus = "On the Way"
                }
                atPickup && !atDestination && location.distanceTo(dropLoc) <= 20 -> {
                    deliveryRef.update("arrivedAtDestination", true)
                    currentStatus = "Arrived at Destination"
                }
            }

            // Push status to WebView
            injectStatusToWebView()
        }
    }

    private fun injectLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) return

        firestore.collection("haulerLocations").document(haulerId)
            .set(mapOf("latitude" to lat, "longitude" to lng, "timestamp" to System.currentTimeMillis()))

        webView.evaluateJavascript("window.updateUserLocation($lat, $lng);", null)
    }

    private fun injectStatusToWebView() {
        val js = "window.updateRouteStatus('$currentStatus');"
        webView.evaluateJavascript(js) { Log.d("STATUS_INJECT", it ?: "null") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
