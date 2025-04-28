package com.ucb.capstone.farmnook.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.farmer.FarmerDeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.farmer.FarmerDashboardFragment
import com.ucb.capstone.farmnook.ui.hauler.DeliveryHistoryFragment
import com.ucb.capstone.farmnook.ui.hauler.HaulerDashboardFragment
import com.ucb.capstone.farmnook.ui.hauler.HaulerDeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.message.InboxFragment
import com.ucb.capstone.farmnook.ui.settings.*
import java.text.SimpleDateFormat
import java.util.*

class NavigationBar : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    var activeRequestId: String? = null

    private var userType: String = "farmer"

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
                    FarmerDeliveryStatusFragment().apply {
                        arguments = Bundle().apply { putString("requestId", activeRequestId) }
                    }
                }
                replaceFragment(fragment)
                bottomNavigationView.selectedItemId = R.id.delivery
            } else {
                resetToDashboard()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.home -> resetToDashboard()
                R.id.history -> replaceFragment(DeliveryHistoryFragment())
                R.id.delivery -> {
                    val fragment = if (userType == "Hauler" || userType == "Hauler Business Admin") {
                        val deliveryIdFromIntent = intent.getStringExtra("deliveryId")
                        HaulerDeliveryStatusFragment().apply {
                            arguments = Bundle().apply {
                                putString("deliveryId", deliveryIdFromIntent)
                            }
                        }
                    } else {
                        FarmerDeliveryStatusFragment().apply {
                            arguments = Bundle().apply {
                                putString("requestId", activeRequestId)
                            }
                        }
                    }
                    replaceFragment(fragment)
                }
                R.id.message -> replaceFragment(InboxFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
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
                val activeDoc = querySnapshot.documents.firstOrNull { doc ->
                    val status = doc.getString("status") ?: ""
                    status != "Cancelled" // ✅ Manually check status here!
                }

                activeRequestId = activeDoc?.getString("requestId")
                Log.d("ActiveRequestRestore", "Restored activeRequestId: $activeRequestId")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("ActiveRequestRestore", "Failed to check active delivery: ${e.message}")
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

    fun resetToDashboard() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val dashboardFragment: Fragment = if (userType == "Hauler" || userType == "Hauler Business Admin") {
            HaulerDashboardFragment()
        } else {
            FarmerDashboardFragment()
        }
        replaceFragment(dashboardFragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val fragmentTag = fragment.javaClass.simpleName
        transaction.replace(R.id.navHost, fragment, fragmentTag)
        transaction.addToBackStack(null)
        transaction.commitAllowingStateLoss()
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
                    userType = document.getString("userType") ?: "farmer"

                    val dateJoined = document.getString("dateJoined") ?: ""
                    val formattedDate = formatDateJoined(dateJoined)

                    val headerView: View = navigationView.getHeaderView(0)
                    headerView.findViewById<TextView>(R.id.full_name).text = fullName
                    headerView.findViewById<TextView>(R.id.member_since).text = "Member Since: $formattedDate"

                    val profileImage = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profileImage)

                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .override(100, 100)
                            .placeholder(R.drawable.profile_circle)
                            .error(R.drawable.profile_circle)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.profile_circle)
                    }

                    Log.d("FirestoreDebug", "Fetched userType: $userType")

                    if (userType == "Farmer") {
                        // ONLY Farmers wait for restoreActiveRequestId ✅
                        restoreActiveRequestId {
                            onFinished()
                        }
                    } else {
                        // Haulers / Admins: IMMEDIATELY run onFinished() ✅
                        activeRequestId = null
                        onFinished()
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
