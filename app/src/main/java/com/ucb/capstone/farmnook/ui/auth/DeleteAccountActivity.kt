package com.ucb.capstone.farmnook.ui.auth

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
import com.ucb.capstone.farmnook.R

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private var userCollection: String = ""  // Store the correct collection name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val confirmDeleteButton = findViewById<Button>(R.id.btn_confirm_delete)
        confirmDeleteButton.setOnClickListener { showPasswordDialog() }

        val cancelButton = findViewById<Button>(R.id.btn_cancel_delete)
        cancelButton.setOnClickListener { finish() }
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
                    determineUserCollection(user.uid)
                }
                .addOnFailureListener { e ->
                    Log.e("ReAuth", "Re-authentication failed", e)
                    Toast.makeText(this, "Invalid password. Please try again.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun determineUserCollection(userId: String) {
        database.collection("farmers").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userCollection = "farmers"
                    deleteUserAccount(userId)
                } else {
                    database.collection("users_business_admin").document(userId).get()
                        .addOnSuccessListener { adminDocument ->
                            if (adminDocument.exists()) {
                                userCollection = "users_business_admin"
                                deleteUserAccount(userId)
                            } else {
                                Toast.makeText(this, "User not found in any collection", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeleteAccount", "Error finding user collection", e)
                            Toast.makeText(this, "Error identifying user. Try again.", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DeleteAccount", "Error finding user collection", e)
                Toast.makeText(this, "Error identifying user. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteUserAccount(userId: String) {
        if (userCollection.isEmpty()) {
            Toast.makeText(this, "Error: User type not identified", Toast.LENGTH_SHORT).show()
            return
        }

        database.collection(userCollection).document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d("DeleteAccount", "User data deleted from Firestore")
                firebaseAuth.currentUser?.delete()
                    ?.addOnSuccessListener {
                        Log.d("DeleteAccount", "User deleted from Authentication")
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    ?.addOnFailureListener { e ->
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
