package com.ucb.capstone.farmnook.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.viewmodel.auth.DeleteAccountViewModel

class DeleteAccountActivity : AppCompatActivity() {

    private val viewModel: DeleteAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        findViewById<Button>(R.id.btn_confirm_delete).setOnClickListener {
            showPasswordDialog()
        }

        findViewById<Button>(R.id.btn_cancel_delete).setOnClickListener {
            finish()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun showPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val passwordInput = dialogView.findViewById<EditText>(R.id.edit_password)

        AlertDialog.Builder(this)
            .setTitle("Confirm Account Deletion")
            .setMessage("Enter your password to delete your account.")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordInput.text.toString().trim()
                if (password.isNotEmpty()) {
                    viewModel.reAuthenticateAndDelete(password)
                } else {
                    Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}