package com.ucb.capstone.farmnook.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R

class EditProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumEditText: EditText
    private lateinit var companyNameEditText: EditText
    private lateinit var saveButton: Button

    private var userCollection: String = ""  // Store the correct collection name

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
        backButton.setOnClickListener { finish() }

        fetchUserData()

        saveButton.setOnClickListener { saveProfileChanges() }
    }

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        database.collection("farmers").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userCollection = "farmers"  // Set correct collection
                    loadProfileData(document)
                } else {
                    database.collection("users_business_admin").document(userId).get()
                        .addOnSuccessListener { adminDocument ->
                            if (adminDocument.exists()) {
                                userCollection = "users_business_admin"  // Set correct collection
                                loadProfileData(adminDocument)
                            }
                        }
                        .addOnFailureListener { exception ->
                            exception.printStackTrace()
                            Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProfileData(document: DocumentSnapshot) {
        firstNameEditText.setText(document.getString("firstName") ?: "")
        lastNameEditText.setText(document.getString("lastName") ?: "")
        emailEditText.setText(document.getString("email") ?: "")
        phoneNumEditText.setText(document.getString("phoneNum") ?: "")

        if (userCollection == "users_business_admin") {
            companyNameEditText.visibility = View.VISIBLE
            companyNameEditText.setText(document.getString("companyName") ?: "")
        } else {
            companyNameEditText.visibility = View.GONE
        }
    }

    private fun saveProfileChanges() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        if (userCollection.isEmpty()) {
            Toast.makeText(this, "Error: User type not identified", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mutableMapOf<String, Any>(
            "firstName" to firstNameEditText.text.toString().trim(),
            "lastName" to lastNameEditText.text.toString().trim(),
            "phoneNum" to phoneNumEditText.text.toString().trim()
        )

        // Only update companyName if the user is a Business Admin
        if (userCollection == "users_business_admin") {
            updatedData["companyName"] = companyNameEditText.text.toString().trim()
        }

        database.collection(userCollection).document(userId).update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}

