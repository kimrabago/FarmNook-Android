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
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.farmer.DeliveryStatusFragment
import com.ucb.capstone.farmnook.ui.farmer.FarmerDashboardFragment
import com.ucb.capstone.farmnook.ui.hauler.DeliveryHistoryFragment
import com.ucb.capstone.farmnook.ui.hauler.HaulerDashboardFragment
import com.ucb.capstone.farmnook.ui.message.InboxFragment
import com.ucb.capstone.farmnook.ui.settings.AboutActivity
import com.ucb.capstone.farmnook.ui.settings.FeedbackActivity
import com.ucb.capstone.farmnook.ui.settings.ProfileActivity
import com.ucb.capstone.farmnook.ui.settings.NotificationActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NavigationBar : AppCompatActivity() {
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
                R.id.feedback -> startActivity(Intent(this, FeedbackActivity::class.java))
                R.id.nav_logout -> {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("status", false)
                            .addOnSuccessListener {
                                Log.d("Logout", "✅ Status set to false on logout")
                                FirebaseAuth.getInstance().signOut() // ✅ Sign out AFTER status is set
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Log.e("Logout", "❌ Failed to set status=false on logout", it)
                                // Fallback signOut even if update fails
                                FirebaseAuth.getInstance().signOut()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                    } else {
                        Log.w("Logout", "⚠️ No userId during logout")
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }

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
        val dashboardFragment: Fragment = if (userType == "Hauler Business Admin" || userType == "Hauler"  ) {
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

        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val profileImageUrl = document.getString("profileImageUrl")
                    val firstName = document.getString("firstName") ?: "User"
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName"

                    userType = document.getString("userType") ?: "farmer" // ✅ Fetch userType dynamically
                    Log.d("FirestoreDebug", "Fetched userType: $userType") // Debug log

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
                            .placeholder(R.drawable.profile_circle) // Placeholder while loading
                            .error(R.drawable.profile_circle) // Error image if loading fails
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.profile_circle)
                    }

                    Log.d("FirestoreDebug", "Fetched userType: $userType")
                    // ✅ Now call resetToDashboard() since userType is updated
                    Log.d("DashboardDebug", "userType at resetToDashboard: $userType")
                    resetToDashboard()

                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
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