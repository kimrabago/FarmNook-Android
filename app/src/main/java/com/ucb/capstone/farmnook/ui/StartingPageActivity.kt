package com.ucb.capstone.farmnook.ui

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.menu.BottomNavigationBar

class StartingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }

        // **Check if user is already logged in**
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // **Navigate directly to BottomNavigationBar**
            val intent = Intent(this, BottomNavigationBar::class.java)
            startActivity(intent)
            finish() // Close StartingPageActivity
            return
        }

        // If user is not logged in, show StartingPageActivity
        setContentView(R.layout.activity_starting_page)

        val startBtn = findViewById<Button>(R.id.startButton)
        startBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}