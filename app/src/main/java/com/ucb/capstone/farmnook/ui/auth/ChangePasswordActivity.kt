package com.ucb.capstone.farmnook.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R

class ChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        val updatePassButton = findViewById<Button>(R.id.update_pass)
        updatePassButton.setOnClickListener {

        }
    }
}
