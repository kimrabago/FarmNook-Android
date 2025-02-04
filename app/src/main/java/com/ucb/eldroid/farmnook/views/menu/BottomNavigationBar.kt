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
import com.ucb.eldroid.farmnook.views.hauler.DashboardFragment

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

        // Handle Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.home -> replaceFragment(DashboardFragment())
                R.id.history -> replaceFragment(DashboardFragment())
                R.id.delivery -> replaceFragment(DashboardFragment())
                R.id.message -> replaceFragment(DashboardFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profile, R.id.notification, R.id.subscription, R.id.report, R.id.feedback -> {
                    replaceFragment(DashboardFragment())
                    drawerLayout.closeDrawer(GravityCompat.START)  // Ensure this line is present
                }
            }
            true
        }

        // Load default fragment
        replaceFragment(DashboardFragment())
    }

    // Function to replace fragments
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        val fragmentTag = fragment.javaClass.simpleName
        if (supportFragmentManager.findFragmentByTag(fragmentTag) == null) {
            transaction.replace(R.id.navHost, fragment, fragmentTag)
            transaction.commit()
        }
    }

    // Handle the Drawer Toggle click
    override fun onSupportNavigateUp(): Boolean {
        return if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawers()
            true
        } else {
            // Pass the correct MenuItem (which would be typically passed in onOptionsItemSelected)
            super.onSupportNavigateUp()
        }
    }
}
