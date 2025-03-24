package com.ucb.capstone.farmnook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.ChangePasswordActivity
import com.ucb.capstone.farmnook.ui.auth.DeleteAccountActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var profileImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        profileImage = findViewById(R.id.profileImage)

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
        val userId = firebaseAuth.currentUser?.uid ?: return

        // First, check if the user exists in the "farmers" collection
        database.collection("farmers").document(userId).get(Source.CACHE)
            .addOnSuccessListener { farmerDocument ->
                if (farmerDocument.exists()) {
                    updateUI(farmerDocument, "farmer")
                } else {
                    // If not found, check in the "users_business_admin" collection
                    database.collection("users_business_admin").document(userId).get(Source.CACHE)
                        .addOnSuccessListener { adminDocument ->
                            if (adminDocument.exists()) {
                                updateUI(adminDocument, "business_admin")
                            } else {
                                // Handle case where user is not found in either collection
                                showUserNotFoundMessage()
                            }
                        }
                        .addOnFailureListener { it.printStackTrace() }
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }
    private fun updateUI(document: DocumentSnapshot, userType: String) {
        val firstName = document.getString("firstName") ?: "User"
        val lastName = document.getString("lastName") ?: ""
        val email = document.getString("email") ?: ""
        val phoneNumber = document.getString("phoneNum") ?: ""
        val dateJoined = document.getString("dateJoined") ?: ""
        // **Fetch profile image URL**
        val profileImageUrl = document.getString("profileImageUrl")


        val fullNameTextView: TextView = findViewById(R.id.fullName)
        val emailTextView: TextView = findViewById(R.id.email)
        val phoneNumTextView: TextView = findViewById(R.id.phone_num)
        val dateJoinedTextView: TextView = findViewById(R.id.dateJoined)
        val companyNameTextView: TextView = findViewById(R.id.companyName)
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        val ratingValue: TextView = findViewById(R.id.ratingValue)

        fullNameTextView.text = "$firstName $lastName"
        emailTextView.text = email
        phoneNumTextView.text = phoneNumber
        dateJoinedTextView.text = dateJoined

        // Show company name only for business admins
        if (userType == "business_admin") {
            val companyName = document.getString("companyName") ?: ""
            companyNameTextView.text = companyName
            companyNameTextView.visibility = View.VISIBLE
            ratingBar.visibility = View.VISIBLE
            ratingValue.visibility = View.VISIBLE
        } else {
            companyNameTextView.visibility = View.GONE
            ratingBar.visibility = View.GONE
            ratingValue.visibility = View.GONE
        }

        if (!profileImageUrl.isNullOrEmpty()) {
            // **Load image using Glide**
            Glide.with(this)
                .load(profileImageUrl)
                .override(100, 100)
                .placeholder(R.drawable.profile_circle) // Placeholder while loading
                .error(R.drawable.profile_circle) // Error image if failed
                .into(profileImage)
        } else {
            // Set default profile image if no image is found
            profileImage.setImageResource(R.drawable.profile_circle)
        }
    }

    private fun showUserNotFoundMessage() {
        val fullNameTextView: TextView = findViewById(R.id.fullName)
        val emailTextView: TextView = findViewById(R.id.email)
        val phoneNumTextView: TextView = findViewById(R.id.phone_num)
        val dateJoinedTextView: TextView = findViewById(R.id.dateJoined)
        val companyNameTextView: TextView = findViewById(R.id.companyName)

        fullNameTextView.text = "Unknown User"
        emailTextView.text = ""
        phoneNumTextView.text = ""
        dateJoinedTextView.text = ""

        companyNameTextView.visibility = View.GONE
    }
}
