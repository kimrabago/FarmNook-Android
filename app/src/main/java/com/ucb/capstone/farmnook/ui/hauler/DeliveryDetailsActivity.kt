package com.ucb.capstone.farmnook.ui.hauler

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R

class DeliveryDetailsActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DeliveryDetailsActivity", "onCreate started")

        super.onCreate(savedInstanceState)
        Toast.makeText(this, "🚀 DeliveryDetailsActivity Started", Toast.LENGTH_SHORT).show()

        setContentView(R.layout.activity_delivery_details)

        val pickupAddress = intent.getStringExtra("pickupAddress")
        val destinationAddress = intent.getStringExtra("destinationAddress")
        val pickup = intent.getStringExtra("pickup") ?: ""
        val destination = intent.getStringExtra("destination") ?: ""
        val estimatedTime = intent.getStringExtra("estimatedTime") ?: "N/A"

        // Set delivery info
        findViewById<TextView>(R.id.estimatedTime).text = estimatedTime
        findViewById<TextView>(R.id.provincePickup).text = pickupAddress
        findViewById<TextView>(R.id.provinceDestination).text = destinationAddress

        // Back button
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // Initialize WebView
        webView = findViewById(R.id.mapView)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()


        // Load your deployed Map (React Leaflet)
        val encodedPickup = pickup.replace(" ", "")
        val encodedDrop = destination.replace(" ", "")
        Log.d("DeliveryDetailsActivity", "Encoded Pickup: $encodedPickup")
        Log.d("DeliveryDetailsActivity", "Encoded Drop: $encodedDrop")
        val mapUrl = "https://farmnook-web.vercel.app/map-viewer?pickup=$encodedPickup&drop=$encodedDrop"
        webView.loadUrl(mapUrl)
    }
}
