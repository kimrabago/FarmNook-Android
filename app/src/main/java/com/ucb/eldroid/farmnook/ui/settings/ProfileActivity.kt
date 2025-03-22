package com.ucb.eldroid.farmnook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.ui.auth.ChangePasswordActivity
import com.ucb.eldroid.farmnook.ui.auth.DeleteAccountActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        val editProfileButton = findViewById<ImageView>(R.id.edit_profile_btn)
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        val changePasswordButton = findViewById<Button>(R.id.change_password)
        changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        val deleteAccountButton = findViewById<Button>(R.id.delete_account)
        deleteAccountButton.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java)
            startActivity(intent)
        }
        fetchUserData()
    }

    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        fetchUserData() // Refresh profile data after returning from EditProfileActivity
    }

    override fun onResume() {
        super.onResume()
        fetchUserData() // Ensures profile data refreshes when activity resumes
    }

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            database.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: "User"
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val phoneNumber = document.getString("phoneNum") ?: ""
                        val userType = document.getString("userType") ?: ""
                        val companyName = document.getString("companyName") ?: ""
                        val dateJoined = document.getString("dateJoined") ?: ""

                        val fullName = "$firstName $lastName"

                        // Update UI
                        val fullNameTextView: TextView = findViewById(R.id.fullName)
                        val emailTextView: TextView = findViewById(R.id.email)
                        val phoneNumTextView: TextView = findViewById(R.id.phone_num)
                        val dateJoinedTextView: TextView = findViewById(R.id.dateJoined)
                        val companyNameTextView: TextView = findViewById(R.id.companyName)

                        fullNameTextView.text = fullName
                        emailTextView.text = email
                        phoneNumTextView.text = phoneNumber
                        dateJoinedTextView.text = dateJoined

                        // Show company name only if the user is a business admin
                        if (userType == "Business Admin") {
                            companyNameTextView.text = companyName
                            companyNameTextView.visibility = View.VISIBLE
                        } else {
                            companyNameTextView.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

}
