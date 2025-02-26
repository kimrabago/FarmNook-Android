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
import com.ucb.eldroid.farmnook.views.message.InboxFragment  // Import InboxFragment

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

        // DrawerToggle setup
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable Hamburger Icon

        // Handle Bottom Navigation Clicks
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.home -> replaceFragment(HaulerDashboardFragment())
                R.id.history -> replaceFragment(FarmerDashboardFragment())
                R.id.delivery -> replaceFragment(DeliveryStatusFragment())
                R.id.message -> replaceFragment(InboxFragment()) // Open InboxFragment
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // Handle Navigation Drawer Menu Clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile, R.id.notification, R.id.subscription, R.id.report, R.id.feedback -> {
                    replaceFragment(HaulerDashboardFragment())
                    drawerLayout.closeDrawer(GravityCompat.START)  // Close drawer
                }
            }
            true
        }

        // Load default fragment
        replaceFragment(HaulerDashboardFragment())
    }

    // Function to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val fragmentTag = fragment.javaClass.simpleName
        if (supportFragmentManager.findFragmentByTag(fragmentTag) == null) {
            transaction.replace(R.id.navHost, fragment, fragmentTag)
            transaction.addToBackStack(null) // Ensure back button works
            transaction.commit()
        }
    }

    // Handle Drawer Toggle Click
    override fun onSupportNavigateUp(): Boolean {
        return if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers()
            true
        } else {
            super.onSupportNavigateUp()
        }
    }
}
