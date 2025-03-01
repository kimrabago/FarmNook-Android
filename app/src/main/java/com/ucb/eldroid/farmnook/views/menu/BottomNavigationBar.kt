package com.ucb.eldroid.farmnook.views.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.auth.LoginActivity
import com.ucb.eldroid.farmnook.views.farmer.DeliveryStatusFragment
import com.ucb.eldroid.farmnook.views.hauler.DeliveryHistoryFragment
import com.ucb.eldroid.farmnook.views.hauler.HaulerDashboardFragment
import com.ucb.eldroid.farmnook.views.hauler.subscription.SubscriptionActivity
import com.ucb.eldroid.farmnook.views.message.InboxFragment
import com.ucb.eldroid.farmnook.views.settings.AboutActivity
import com.ucb.eldroid.farmnook.views.settings.FeedbackActivity
import com.ucb.eldroid.farmnook.views.settings.HaulerProfileActivity
import com.ucb.eldroid.farmnook.views.settings.NotificationActivity
import com.ucb.eldroid.farmnook.views.settings.ReportActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation_bar)

        // Initialize Views
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

        // Fetch and display user data in navigation header
        fetchUserData()

        // Handle Bottom Navigation Clicks
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.home -> resetToDashboard() // Always reset dashboard
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
            resetToDashboard()
        }
    }

    // Function to reset to dashboard (Home)
    private fun resetToDashboard() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE) // Clear back stack
        replaceFragment(HaulerDashboardFragment())
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
        startActivity(Intent(this, HaulerProfileActivity::class.java))
    }

    // Fetch and display user data in navigation header
    @SuppressLint("SetTextI18n")
    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            database.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: "User"
                        val lastName = document.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName"

                        // ✅ Get dateJoined and format it
                        val dateJoined = document.getString("dateJoined") ?: ""
                        val formattedDate = formatDateJoined(dateJoined)

                        // Update UI in the navigation header
                        val headerView: View = navigationView.getHeaderView(0)
                        val fullNameTextView: TextView = headerView.findViewById(R.id.full_name)
                        val memberSinceTextView: TextView = headerView.findViewById(R.id.member_since)

                        fullNameTextView.text = fullName
                        memberSinceTextView.text = "Member Since: $formattedDate" // ✅ Show formatted date

                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    // Function to format dateJoined as "January 2025"
    private fun formatDateJoined(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // January 2025 format
            val date = inputFormat.parse(dateString) // Convert to Date
            outputFormat.format(date ?: Date()) // Format as "January 2025"
        } catch (e: Exception) {
            "" // Return empty string if parsing fails
        }
    }

    // Handle Drawer Back Button
    override fun onSupportNavigateUp(): Boolean {
        return if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
            true
        } else {
            onBackPressed()
            true
        }
    }
}
