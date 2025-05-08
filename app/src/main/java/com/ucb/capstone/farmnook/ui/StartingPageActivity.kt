package com.ucb.capstone.farmnook.ui

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.LoginActivity
import com.ucb.capstone.farmnook.ui.menu.NavigationBar

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

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, NavigationBar::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_starting_page)

        val startBtn = findViewById<Button>(R.id.startButton)
        startBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}