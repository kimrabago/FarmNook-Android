package com.ucb.eldroid.farmnook.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        // Handle back button click
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Go back to the previous screen
        }

        // Handle the Update Password button click
        val updatePassButton = findViewById<Button>(R.id.update_pass)
        updatePassButton.setOnClickListener {

        }
    }
}
