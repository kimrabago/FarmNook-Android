package com.ucb.eldroid.farmnook.views.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.hauler.DashboardFragment

class BottomNavigationBar : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation_bar)

        bottomNavigationView = findViewById(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener { menu ->
            when(menu.itemId){
                R.id.home -> {
                    replaceFragment(DashboardFragment())
                    true
                }R.id.history -> {
                replaceFragment(DashboardFragment())
                true
            }

                R.id.delivery -> {
                replaceFragment(DashboardFragment())
                true
            }R.id.message -> {
                replaceFragment(DashboardFragment())
                true
            }
                else -> false
            }
        }
        replaceFragment(DashboardFragment())
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHost, fragment)
            .commit()
    }
}
