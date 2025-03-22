package com.ucb.eldroid.farmnook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.ui.auth.ChangePasswordActivity
import com.ucb.eldroid.farmnook.ui.auth.DeleteAccountActivity

class EditProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumEditText: EditText
    private lateinit var companyNameEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        firstNameEditText = findViewById(R.id.first_name)
        lastNameEditText = findViewById(R.id.last_name)
        emailEditText = findViewById(R.id.email)
        phoneNumEditText = findViewById(R.id.phone_num)
        companyNameEditText = findViewById(R.id.company_name)
        saveButton = findViewById(R.id.save_profile_btn)

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        fetchUserData()

        saveButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            database.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        firstNameEditText.setText(document.getString("firstName") ?: "User")
                        lastNameEditText.setText(document.getString("lastName") ?: "")
                        emailEditText.setText(document.getString("email") ?: "")
                        phoneNumEditText.setText(document.getString("phoneNum") ?: "")
                        companyNameEditText.setText(document.getString("companyName") ?: "")
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveProfileChanges() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val updatedData = mapOf(
                "firstName" to firstNameEditText.text.toString(),
                "lastName" to lastNameEditText.text.toString(),
                "email" to emailEditText.text.toString(),
                "phoneNum" to phoneNumEditText.text.toString(),
                "companyName" to companyNameEditText.text.toString()
            )

            database.collection("users").document(userId).update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity after saving
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
