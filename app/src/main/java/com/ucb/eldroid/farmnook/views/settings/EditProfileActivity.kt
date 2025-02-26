package com.ucb.eldroid.farmnook.views.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.auth.ChangePasswordActivity
import com.ucb.eldroid.farmnook.views.auth.DeleteAccountActivity

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Back button handling
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Close the screen when back button is clicked
        }

        // Navigate to ChangePasswordActivity when the Change Password button is clicked
        val changePasswordButton = findViewById<Button>(R.id.change_password)
        changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Navigate to DeleteAccountActivity when the Delete Account button is clicked
        val deleteAccountButton = findViewById<Button>(R.id.delete_account)
        deleteAccountButton.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java)
            startActivity(intent)
        }
    }
}
