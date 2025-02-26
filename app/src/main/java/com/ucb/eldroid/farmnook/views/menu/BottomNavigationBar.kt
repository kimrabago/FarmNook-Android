package com.ucb.eldroid.farmnook.views.menu

import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.farmer.DeliveryStatusFragment
import com.ucb.eldroid.farmnook.views.farmer.FarmerDashboardFragment
import com.ucb.eldroid.farmnook.views.hauler.HaulerDashboardFragment
import com.ucb.eldroid.farmnook.views.message.InboxFragment

class BottomNavigationBar : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
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
                R.id.home -> resetToDashboard() // Always reset dashboard
                R.id.history -> replaceFragment(FarmerDashboardFragment())
                R.id.delivery -> replaceFragment(DeliveryStatusFragment())
                R.id.message -> replaceFragment(InboxFragment())
                else -> return@setOnItemSelectedListener false
            }
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


    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack() // Go to previous fragment
        } else {
            finish() // Exit app if already on Home
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
