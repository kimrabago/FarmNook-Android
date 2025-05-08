package com.ucb.capstone.farmnook.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.users.farmer.FarmerDeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.users.farmer.FarmerDashboardFragment
import com.ucb.capstone.farmnook.ui.users.DeliveryHistoryFragment
import com.ucb.capstone.farmnook.ui.users.hauler.HaulerDashboardFragment
import com.ucb.capstone.farmnook.ui.users.hauler.HaulerDeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.message.InboxFragment
import com.ucb.capstone.farmnook.ui.settings.*
import com.ucb.capstone.farmnook.utils.loadImage
import java.text.SimpleDateFormat
import java.util.*

class NavigationBar : AppCompatActivity() {

    private var currentFragmentTag: String? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    var activeRequestId: String? = null
    private var activeDeliveryId: String? = null

    private var userType: String = "farmer"

    private var pickupName: String? = null
    private var destinationName: String? = null
    private var productType: String? = null
    private var purpose: String? = null
    private var weight: String? = null
    private var totalCost: Double = -1.0
    private var estimatedTime: String? = null
    private var businessId: String? = null
    private var vehicleId: String? = null
    private var businessName: String? = null
    private var locationName: String? = null
    private var profileImage: String? = null
    private var vehicleType: String? = null
    private var vehicleModel: String? = null
    private var plateNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation_bar)

        bottomNavigationView = findViewById(R.id.bottom_nav)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupBottomNavigation()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile -> navigateToProfile()
                R.id.notification -> startActivity(Intent(this, NotificationActivity::class.java))
                R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.feedback -> startActivity(Intent(this, FeedbackActivity::class.java))
                R.id.nav_logout -> handleLogout()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchUserData {
            val navigateTo = intent.getStringExtra("navigateTo")
            val deliveryId = intent.getStringExtra("deliveryId")

            if (navigateTo == "DeliveryStatus") {
                val fragment = if (userType == "Hauler" || userType == "Hauler Business Admin") {
                    HaulerDeliveryStatusFragment().apply {
                        arguments = Bundle().apply { putString("deliveryId", deliveryId) }
                    }
                } else {
                    activeRequestId = intent.getStringExtra("requestId")
                    pickupName = intent.getStringExtra("pickupName") ?: pickupName
                    destinationName = intent.getStringExtra("destinationName") ?: destinationName
                    productType = intent.getStringExtra("productType") ?: productType
                    purpose = intent.getStringExtra("purpose") ?: purpose
                    weight = intent.getStringExtra("weight") ?: weight
                    totalCost = intent.getDoubleExtra("estimatedCost", totalCost)
                    estimatedTime = intent.getStringExtra("estimatedTime") ?: estimatedTime
                    businessId = intent.getStringExtra("businessId") ?: businessId
                    vehicleId = intent.getStringExtra("vehicleId") ?: vehicleId
                    businessName = intent.getStringExtra("businessName") ?: businessName
                    locationName = intent.getStringExtra("locationName") ?: locationName
                    profileImage = intent.getStringExtra("profileImageUrl") ?: profileImage
                    vehicleType = intent.getStringExtra("vehicleType") ?: vehicleType
                    vehicleModel = intent.getStringExtra("vehicleModel") ?: vehicleModel
                    plateNumber = intent.getStringExtra("plateNumber") ?: plateNumber

                    FarmerDeliveryStatusFragment().apply {
                        arguments = Bundle().apply { putString("requestId", activeRequestId)
                            putString("pickupName", pickupName)
                            putString("destinationName", destinationName)
                            putString("purpose", purpose)
                            putString("productType", productType)
                            putString("weight", weight)
                            putDouble("estimatedCost", totalCost)
                            putString("estimatedTime", estimatedTime)
                            putString("businessName", businessName)
                            putString("locationName", locationName)
                            putString("profileImageUrl", profileImage)
                            putString("vehicleType", vehicleType)
                            putString("vehicleModel", vehicleModel)
                            putString("plateNumber", plateNumber)
                        }
                    }
                }
                replaceFragment(fragment, "Delivery")
                bottomNavigationView.selectedItemId = R.id.delivery
            } else {
                resetToDashboard()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { menu ->
            val selectedTag = when (menu.itemId) {
                R.id.home -> "Dashboard"
                R.id.history -> "History"
                R.id.delivery -> "Delivery"
                R.id.message -> "Inbox"
                else -> null
            }

            if (selectedTag == currentFragmentTag) return@setOnItemSelectedListener false

            when (menu.itemId) {
                R.id.home -> resetToDashboard()
                R.id.history -> replaceFragment(DeliveryHistoryFragment(), "History")
                R.id.delivery -> {
                    val fragment = if (userType == "Hauler" || userType == "Hauler Business Admin") {
                        val bundle = Bundle().apply {
                            putString("deliveryId", activeDeliveryId)
                        }
                        HaulerDeliveryStatusFragment().apply { arguments = bundle
                            Log.d("NAV", "✅ Pass deliveryId: $activeDeliveryId")}
                    } else {
                        FarmerDeliveryStatusFragment().apply {
                            arguments = Bundle().apply {
                                putString("requestId", activeRequestId)
                                putString("pickupName", pickupName)
                                putString("destinationName", destinationName)
                                putString("purpose", purpose)
                                putString("productType", productType)
                                putString("weight", weight)
                                putDouble("estimatedCost", totalCost)
                                putString("estimatedTime", estimatedTime)
                                putString("businessName", businessName)
                                putString("locationName", locationName)
                                putString("profileImageUrl", profileImage)
                                putString("vehicleType", vehicleType)
                                putString("vehicleModel", vehicleModel)
                                putString("plateNumber", plateNumber)
                            }
                        }
                    }
                    replaceFragment(fragment, "Delivery")
                }
                R.id.message -> replaceFragment(InboxFragment(), "Inbox")
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        currentFragmentTag = tag
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHost, fragment, tag)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun resetToDashboard() {
        if (supportFragmentManager.isStateSaved) {
            Handler(Looper.getMainLooper()).post { resetToDashboard() }
        } else {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            val dashboardFragment = if (userType == "Hauler" || userType == "Hauler Business Admin") {
                HaulerDashboardFragment()
            } else {
                FarmerDashboardFragment()
            }
            replaceFragment(dashboardFragment, "Dashboard")
        }
    }

    fun restoreActiveRequestId(onComplete: () -> Unit) {
        if (userType != "Farmer") {
            activeRequestId = null
            onComplete()
            return
        }

        val userId = firebaseAuth.currentUser?.uid ?: return

        database.collection("deliveryRequests")
            .whereEqualTo("farmerId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->

                val requestDocs = querySnapshot.documents
                if (requestDocs.isEmpty()) {
                    activeRequestId = null
                    onComplete()
                    return@addOnSuccessListener
                }

                // Go through each deliveryRequest and check if its delivery is not done
                val iterator = requestDocs.iterator()

                fun checkNext() {
                    if (!iterator.hasNext()) {
                        activeRequestId = null
                        onComplete()
                        return
                    }

                    val doc = iterator.next()
                    val requestId = doc.getString("requestId") ?: return@checkNext
                    val status = doc.getString("status") ?: ""

                    if (status == "Cancelled") {
                        checkNext()
                        return
                    }

                    database.collection("deliveries")
                        .whereEqualTo("requestId", requestId)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { deliverySnapshot ->
                            val deliveryDoc = deliverySnapshot.documents.firstOrNull()
                            val isDone = deliveryDoc?.getBoolean("isDone") ?: false

                            if (isDone) {
                                checkNext()
                            } else {
                                // Found valid ongoing delivery
                                activeRequestId = requestId
                                pickupName = doc.getString("pickupName")
                                destinationName = doc.getString("destinationName")
                                purpose = doc.getString("purpose")
                                productType = doc.getString("productType")
                                weight = doc.getString("weight")
                                totalCost = doc.getDouble("estimatedCost") ?: -1.0
                                estimatedTime = doc.getString("estimatedTime")
                                businessId = doc.getString("businessId")
                                vehicleId = doc.getString("vehicleId")

                                // Load business and vehicle details
                                if (!businessId.isNullOrEmpty()) {
                                    database.collection("users").document(businessId!!)
                                        .get()
                                        .addOnSuccessListener { businessDoc ->
                                            businessName = businessDoc.getString("businessName")
                                            locationName = businessDoc.getString("locationName")
                                            profileImage = businessDoc.getString("profileImageUrl")

                                            if (!vehicleId.isNullOrEmpty()) {
                                                database.collection("vehicles").document(vehicleId!!)
                                                    .get()
                                                    .addOnSuccessListener { vehicleDoc ->
                                                        vehicleType = vehicleDoc.getString("vehicleType")
                                                        vehicleModel = vehicleDoc.getString("model")
                                                        plateNumber = vehicleDoc.getString("plateNumber")
                                                        onComplete()
                                                    }
                                                    .addOnFailureListener { onComplete() }
                                            } else {
                                                onComplete()
                                            }
                                        }
                                        .addOnFailureListener { onComplete() }
                                } else {
                                    onComplete()
                                }
                            }
                        }
                        .addOnFailureListener {
                            checkNext()
                        }
                }

                checkNext()
            }
            .addOnFailureListener {
                activeRequestId = null
                onComplete()
            }
    }

    private fun handleLogout() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            database.collection("users").document(userId)
                .update("status", false)
                .addOnSuccessListener {
                    firebaseAuth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    firebaseAuth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        } else {
            firebaseAuth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    @SuppressLint("SetTextI18n")
    private fun fetchUserData(onFinished: () -> Unit) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileImageUrl = document.getString("profileImageUrl")
                    val firstName = document.getString("firstName") ?: "User"
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName"
                    val dateJoined = document.getString("dateJoined") ?: ""
                    val formattedDate = formatDateJoined(dateJoined)
                    userType = document.getString("userType") ?: "farmer"

                    val headerView: View = navigationView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.full_name).text = fullName
                    headerView.findViewById<TextView>(R.id.member_since).text = "Member Since: $formattedDate"

                    val profileImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)
                    profileImage.loadImage(profileImageUrl)

                    if (userType == "Farmer") {
                        restoreActiveRequestId { onFinished() }
                    } else {
                        database.collection("deliveries")
                            .whereEqualTo("haulerAssignedId", userId)
                            .whereEqualTo("isStarted", true)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val active = snapshot.documents.firstOrNull { doc ->
                                    val done = doc.getBoolean("isDone") ?: false
                                    !done
                                }

                                activeDeliveryId = active?.id
                                Log.d("NAV", "✅ Restored activeDeliveryId: $activeDeliveryId")
                                onFinished()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Failed to fetch user", e)
                onFinished()
            }
    }

    private fun formatDateJoined(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "N/A"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
            true
        } else {
            onBackPressedDispatcher.onBackPressed()
            true
        }
    }

}