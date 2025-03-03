package com.ucb.eldroid.farmnook.views.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.auth.LoginActivity

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val confirmDeleteButton = findViewById<Button>(R.id.btn_confirm_delete)
        confirmDeleteButton.setOnClickListener {
            showPasswordDialog()
        }

        val cancelButton = findViewById<Button>(R.id.btn_cancel_delete)
        cancelButton.setOnClickListener {
            finish()
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
                    reAuthenticateAndDelete(password)
                } else {
                    Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reAuthenticateAndDelete(password: String) {
        val user = firebaseAuth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)

            user.reauthenticate(credential)
                .addOnSuccessListener {
                    Log.d("ReAuth", "User re-authenticated successfully.")
                    deleteUserAccount()
                }
                .addOnFailureListener { e ->
                    Log.e("ReAuth", "Re-authentication failed", e)
                    Toast.makeText(this, "Invalid password. Please try again.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun deleteUserAccount() {
        val user = firebaseAuth.currentUser
        val userId = user?.uid

        if (userId != null) {
            // ✅ 1. Delete user data from Firestore
            database.collection("users").document(userId)
                .delete()
                .addOnSuccessListener {
                    Log.d("DeleteAccount", "User data deleted from Firestore")

                    // ✅ 2. Delete user from Firebase Authentication
                    user.delete()
                        .addOnSuccessListener {
                            Log.d("DeleteAccount", "User deleted from Authentication")
                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()

                            // ✅ Redirect to Login Activity after deletion
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeleteAccount", "Failed to delete user from Authentication", e)
                            Toast.makeText(this, "Account deletion failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("DeleteAccount", "Failed to delete user data from Firestore", e)
                    Toast.makeText(this, "Failed to delete account data. Try again.", Toast.LENGTH_LONG).show()
                }
        }
    }
}
