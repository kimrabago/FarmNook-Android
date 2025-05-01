package com.ucb.capstone.farmnook.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var profileImage: ImageView
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneNumEditText: EditText
    private lateinit var businessNameEditText: EditText
    private lateinit var saveButton: Button

    private var imageUri: Uri? = null
    private var selectedFileName: String? = null

    companion object {
        const val IMAGE_PICK_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        profileImage = findViewById(R.id.profileImage)
        firstNameEditText = findViewById(R.id.first_name)
        lastNameEditText = findViewById(R.id.last_name)
        emailEditText = findViewById(R.id.email)
        phoneNumEditText = findViewById(R.id.phone_num)
        businessNameEditText = findViewById(R.id.business_name)
        saveButton = findViewById(R.id.save_profile_btn)

        val locationEditText = findViewById<EditText>(R.id.location)
        val locationLabel = findViewById<TextView>(R.id.locationLabel)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_REQUEST)
        }

        viewModel.fetchUserData()

        viewModel.userProfile.observe(this) { profile ->
            firstNameEditText.setText(profile.user.firstName)
            lastNameEditText.setText(profile.user.lastName)
            emailEditText.setText(profile.user.email)
            phoneNumEditText.setText(profile.user.phoneNum)

            val userType = profile.user.userType
            if (userType == "Hauler Business Admin") {
                businessNameEditText.visibility = View.VISIBLE
                businessNameEditText.setText(profile.businessName)
                businessNameEditText.isEnabled = true
            } else if (userType == "Hauler") {
                businessNameEditText.visibility = View.VISIBLE
                businessNameEditText.setText(profile.businessName)
                businessNameEditText.isEnabled = false
            } else {
                businessNameEditText.visibility = View.GONE
            }

            Glide.with(this)
                .load(profile.profileImageUrl)
                .placeholder(R.drawable.profile_circle)
                .error(R.drawable.profile_circle)
                .into(profileImage)
        }

        viewModel.profileImageUri.observe(this) { url ->
            Glide.with(this).load(url).into(profileImage)
        }

        viewModel.showLocation.observe(this) { isVisible ->
            locationEditText.visibility = if (isVisible) View.VISIBLE else View.GONE
            locationLabel.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val phone = phoneNumEditText.text.toString()
            val businessName = businessNameEditText.text.toString()
            val showBusiness = businessNameEditText.visibility == View.VISIBLE

            viewModel.saveProfileData(firstName, lastName, email, phone, businessName, showBusiness)

            imageUri?.let { uri ->
                val fileName = selectedFileName ?: "profile.jpg"
                viewModel.uploadProfileImage(uri, fileName)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_REQUEST) {
            imageUri = data?.data
            profileImage.setImageURI(imageUri)
            selectedFileName = getFileName(imageUri!!)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                }
            }
        }
        return result ?: uri.lastPathSegment ?: "default.jpg"
    }
}
