package com.ucb.eldroid.farmnook.views.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.menu.BottomNavigationBar

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val forgotPasswordTextView = findViewById<TextView>(R.id.tv_forgot_password)
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        val signUpTextView = findViewById<TextView>(R.id.tv_sign_up)
        signUpTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Navigate to BottomNavigationBar when login button is clicked
        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.setOnClickListener {
            val intent = Intent(this, BottomNavigationBar::class.java)
            startActivity(intent)
            finish() // This prevents the user from going back to the login screen after logging in
        }
    }
}
