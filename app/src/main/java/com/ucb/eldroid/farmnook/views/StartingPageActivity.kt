package com.ucb.eldroid.farmnook.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.auth.LoginActivity

class StartingPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starting_page)

        val startBtn = findViewById<Button>(R.id.startButton)

        startBtn.setOnClickListener { v: View? ->
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}