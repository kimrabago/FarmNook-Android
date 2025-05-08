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

    private lateinit var viewModel: FarmerDashboardViewModel

    private lateinit var menuBurger: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var addDeliveryBtn: Button
    private lateinit var webView: WebView

    private var webViewLoaded = false

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

        viewModel = ViewModelProvider(this)[FarmerDashboardViewModel::class.java]

        observeViewModel()
        setupWebView()
        setupClickListeners()
        viewModel.fetchProfileImage()

        return rootView
    }

    private fun observeViewModel() {
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            profileIcon.loadImage(imageUrl)
        }

        viewModel.location.observe(viewLifecycleOwner) { location ->
            location?.let {
                sendLocationToWebView(it.latitude, it.longitude)
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
                webViewLoaded = true
                checkPermissionsAndStartLocation()
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
            if (isGPSEnabled(requireContext())) {
                viewModel.startLocationUpdates()
                viewModel.getLastLocation()
            } else {
                Toast.makeText(requireContext(), "Please enable GPS!", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun sendLocationToWebView(lat: Double, lng: Double) {
        if (!webViewLoaded) return
        webView.post {
            val js = "window.updateUserLocation($lat, $lng);"
            webView.evaluateJavascript(js, null)
        }
    }

    private fun checkActiveRequestStatus() {
        val activity = activity as? NavigationBar
        val activeRequestId = activity?.let { it.activeRequestId }

        if (!activeRequestId.isNullOrEmpty()) {
            addDeliveryBtn.visibility = View.GONE
        } else {
            addDeliveryBtn.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        checkActiveRequestStatus()
        viewModel.getLastLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopLocationUpdates()
    }
}
