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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.farmer.DeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.farmer.FarmerDashboardFragment
import com.ucb.capstone.farmnook.ui.hauler.DeliveryHistoryFragment
import com.ucb.capstone.farmnook.ui.hauler.HaulerDashboardFragment
import com.ucb.capstone.farmnook.ui.hauler.subscription.SubscriptionActivity
import com.ucb.capstone.farmnook.ui.message.InboxFragment
import com.ucb.capstone.farmnook.ui.settings.AboutActivity
import com.ucb.capstone.farmnook.ui.settings.FeedbackActivity
import com.ucb.capstone.farmnook.ui.settings.ProfileActivity
import com.ucb.capstone.farmnook.ui.settings.NotificationActivity
import com.ucb.capstone.farmnook.ui.settings.ReportActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BottomNavigationBar : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private var userType: String = "farmer"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation_bar)

        bottomNavigationView = findViewById(R.id.bottom_nav)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        // Drawer Toggle setup
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button in toolbar

        // Handle Bottom Navigation Clicks
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.home -> resetToDashboard()
                R.id.history -> replaceFragment(DeliveryHistoryFragment())
                R.id.delivery -> replaceFragment(DeliveryStatusFragment())
                R.id.message -> replaceFragment(InboxFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // Handle Navigation Menu Clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile -> navigateToProfile()
                R.id.notification -> startActivity(Intent(this, NotificationActivity::class.java))
                R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
                R.id.subscription -> startActivity(Intent(this, SubscriptionActivity::class.java))
                R.id.report -> startActivity(Intent(this, ReportActivity::class.java))
                R.id.feedback -> startActivity(Intent(this, FeedbackActivity::class.java))
                R.id.nav_logout -> {
                    firebaseAuth.signOut() // Logout user
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START) // Close drawer after selection
            true
        }

        // Load default fragment
        if (savedInstanceState == null) {
            Log.d("FirestoreDebug", "Updated userType: $userType") // Debug
            resetToDashboard()
        }
        // Fetch and display user data in navigation header
        fetchUserData()
    }

    // Function to reset to dashboard (Home)
    private fun resetToDashboard() {
        Log.d("DashboardDebug", "Resetting to Dashboard. userType: $userType")
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val dashboardFragment: Fragment = if (userType == "Business Admin") {
            Log.d("DashboardDebug", "Loading HaulerDashboardFragment")
            HaulerDashboardFragment()
        } else {
            Log.d("DashboardDebug", "Loading FarmerDashboardFragment")
            FarmerDashboardFragment()
        }
        replaceFragment(dashboardFragment)
    }

    // Function to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val fragmentTag = fragment.javaClass.simpleName
        transaction.replace(R.id.navHost, fragment, fragmentTag)
        transaction.addToBackStack(null) // Ensures back navigation works
        transaction.commit()
    }

    // Handle Profile Navigation from Fragment
    fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    // Fetch and display user data in navigation header
    @SuppressLint("SetTextI18n")
    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        val farmersRef = database.collection("farmers").document(userId)
        val adminRef = database.collection("users_business_admin").document(userId)

        farmersRef.get().addOnSuccessListener { farmerDoc ->
            if (farmerDoc.exists()) {
                val firstName = farmerDoc.getString("firstName") ?: "User"
                val lastName = farmerDoc.getString("lastName") ?: ""
                val fullName = "$firstName $lastName"

                userType = "farmer" // ✅ Set userType to "farmer"
                updateUI(fullName, farmerDoc.getString("dateJoined") ?: "")
            } else {
                adminRef.get().addOnSuccessListener { adminDoc ->
                    if (adminDoc.exists()) {
                        val firstName = adminDoc.getString("firstName") ?: "User"
                        val lastName = adminDoc.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"

                        userType = "Business Admin" // ✅ Set userType to "Business Admin"
                        updateUI(fullName, adminDoc.getString("dateJoined") ?: "")
                    } else {
                        Log.e("FirestoreDebug", "User not found in both collections.")
                    }
                }.addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
    }

    // ✅ Separate function to update UI
    private fun updateUI(fullName: String, dateJoined: String) {
        val formattedDate = formatDateJoined(dateJoined)

        val headerView: View = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.full_name).text = fullName
        headerView.findViewById<TextView>(R.id.member_since).text = "Member Since: $formattedDate"

        // Show subscription menu only for Business Admin
        val menu = navigationView.menu
        val subscriptionMenuItem = menu.findItem(R.id.subscription)
        subscriptionMenuItem.isVisible = userType == "Business Admin"

        Log.d("FirestoreDebug", "Fetched userType: $userType")

        // ✅ Now call resetToDashboard() since userType is updated
        resetToDashboard()
    }


    // Function to format dateJoined as "January 2025"
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