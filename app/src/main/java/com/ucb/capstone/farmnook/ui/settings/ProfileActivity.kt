package com.ucb.capstone.farmnook.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
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

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            database.collection("users").document(userId).get(Source.CACHE)
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: "User"
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val phoneNumber = document.getString("phoneNum") ?: ""
                        val userType = document.getString("userType") ?: ""
                        val businessID = document.getString("businessId")
                        val dateJoined = document.getString("dateJoined") ?: ""
                        val profileImageUrl = document.getString("profileImageUrl")

                        val fullName = "$firstName $lastName"

                        // Update UI
                        val fullNameTextView: TextView = findViewById(R.id.fullName)
                        val emailTextView: TextView = findViewById(R.id.email)
                        val phoneNumTextView: TextView = findViewById(R.id.phone_num)
                        val dateJoinedTextView: TextView = findViewById(R.id.dateJoined)
                        val businessNameTextView: TextView = findViewById(R.id.businessName)

                        fullNameTextView.text = fullName
                        emailTextView.text = email
                        phoneNumTextView.text = phoneNumber
                        dateJoinedTextView.text = dateJoined


                        if (userType == "Hauler" && !businessID.isNullOrEmpty()) {
                            database.collection("users").document(businessID).get(Source.CACHE)
                                .addOnSuccessListener { adminDoc ->
                                    if (adminDoc.exists()) {
                                        val businessName = adminDoc.getString("businessName") ?: "N/A"
                                        runOnUiThread {
                                            businessNameTextView.text = businessName
                                            businessNameTextView.visibility = View.VISIBLE
                                        }
                                        Log.d("ProfileActivity", "businessID: $businessID")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    exception.printStackTrace()
                                }
                        } else {
                            businessNameTextView.visibility = View.GONE
                        }


                        if (!profileImageUrl.isNullOrEmpty()) {
                            // **Load image using Glide**
                            Glide.with(this)
                                .load(profileImageUrl)
                                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both the original and resized images
                                .override(100, 100) // Define a fixed image size
                                .placeholder(R.drawable.profile_circle)
                                .error(R.drawable.profile_circle)
                                .into(profileImage)
                        } else {
                            // Set default profile image if no image is found
                            profileImage.setImageResource(R.drawable.profile_circle)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }
    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            fetchUserData() // Refresh profile data after returning from EditProfileActivity
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserData() // Ensures profile data refreshes when activity resumes
    }

}
