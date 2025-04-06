package com.ucb.capstone.farmnook.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.ucb.capstone.farmnook.R

class EditProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var storageReference: StorageReference

    private lateinit var profileImage: ImageView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumEditText: EditText
    private lateinit var businessNameEditText: EditText
    private lateinit var saveButton: Button

    private var projectImageUri: Uri? = null
    private var selectedFileName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        firstNameEditText = findViewById(R.id.first_name)
        lastNameEditText = findViewById(R.id.last_name)
        emailEditText = findViewById(R.id.email)
        phoneNumEditText = findViewById(R.id.phone_num)
        businessNameEditText = findViewById(R.id.business_name)
        saveButton = findViewById(R.id.save_profile_btn)
        profileImage = findViewById(R.id.profileImage)

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_REQUEST)
        }

        fetchUserData()

        saveButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_REQUEST) {
            data?.data?.let { uri ->
                projectImageUri = uri
                profileImage.setImageURI(uri)

                // Get filename from URI
                val fileName = getFileName(uri)
                selectedFileName = fileName  // Display the image in the ImageView
            }
        }
    }
    // Function to get filename from URI
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result ?: "default_filename.jpg"
    }

    companion object {
        const val IMAGE_PICK_REQUEST = 1001
    }

    private fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            database.collection("users").document(userId).get(Source.CACHE)
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        firstNameEditText.setText(document.getString("firstName") ?: "User")
                        lastNameEditText.setText(document.getString("lastName") ?: "")
                        emailEditText.setText(document.getString("email") ?: "")
                        phoneNumEditText.setText(document.getString("phoneNum") ?: "")

                        val userType = document.getString("userType") ?: ""
                        val businessName = document.getString("businessName") ?: ""
                        val businessID = document.getString("businessAdminId")

                        if (userType == "Hauler") {
                            // Make businessName a TextView for Hauler users (not editable)
                            businessNameEditText.visibility = View.VISIBLE
                            businessNameEditText.setText(businessName, TextView.BufferType.NORMAL)
                            businessNameEditText.isEnabled = false // Disable editing

                        } else if (userType == "Hauler Business Admin") {
                            // Make businessNameEditText editable for Hauler Business Admin
                            businessNameEditText.visibility = View.VISIBLE
                            businessNameEditText.setText(businessName, TextView.BufferType.EDITABLE)
                            businessNameEditText.isEnabled = true // Enable editing
                        } else {
                            // Hide businessName for other user types (optional, based on your design)
                            businessNameEditText.visibility = View.GONE
                        }

                        if (userType == "Hauler" && !businessID.isNullOrEmpty()) {
                            database.collection("users").document(businessID).get(Source.CACHE)
                                .addOnSuccessListener { adminDoc ->
                                    if (adminDoc.exists()) {
                                        val businessName = adminDoc.getString("businessName") ?: "N/A"
                                        runOnUiThread {
                                            businessNameEditText.setText(businessName, TextView.BufferType.EDITABLE)
                                            businessNameEditText.visibility = View.VISIBLE
                                        }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    exception.printStackTrace()
                                }
                        } else {
                            businessNameEditText.visibility = View.GONE
                        }

                        // **Fetch profile image URL**
                        val profileImageUrl = document.getString("profileImageUrl")

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
            val userDocRef = database.collection("users").document(userId)

            if (projectImageUri != null) {
                val fileName = selectedFileName ?: "$userId.jpg"
                val imageRef = storageReference.child("profileImages/$userId/$fileName")

                imageRef.putFile(projectImageUri!!)
                    .addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            userDocRef.update("profileImageUrl", uri.toString())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show()
                                    // Notify ProfileActivity to reload the profile
                                    setResult(RESULT_OK)
                                    finish()
                                }
                                .addOnFailureListener { exception ->
                                    exception.printStackTrace()
                                    Toast.makeText(this, "Failed to update image URL", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        exception.printStackTrace()
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
            }

            userDocRef.update(
                "firstName", firstNameEditText.text.toString(),
                "lastName", lastNameEditText.text.toString(),
                "email", emailEditText.text.toString(),
                "phoneNum", phoneNumEditText.text.toString(),
                "businessName", if (businessNameEditText.visibility == View.VISIBLE) businessNameEditText.text.toString() else ""
            ).addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { exception ->
                exception.printStackTrace()
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
