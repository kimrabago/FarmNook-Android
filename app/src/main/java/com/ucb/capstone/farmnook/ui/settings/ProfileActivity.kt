package com.ucb.capstone.farmnook.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.auth.ChangePasswordActivity
import com.ucb.capstone.farmnook.ui.auth.DeleteAccountActivity
import com.ucb.capstone.farmnook.viewmodel.settings.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var viewModel: ProfileViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)
        profileImage = findViewById(R.id.profileImage)

        val fullNameTextView: TextView = findViewById(R.id.fullName)
        val emailTextView: TextView = findViewById(R.id.email)
        val phoneNumTextView: TextView = findViewById(R.id.phone_num)
        val dateJoinedTextView: TextView = findViewById(R.id.dateJoined)
        val businessNameTextView: TextView = findViewById(R.id.businessName)

        viewModel.userProfile.observe(this) { profile ->
            val user = profile.user
            fullNameTextView.text = "${user.firstName} ${user.lastName}"
            emailTextView.text = user.email
            phoneNumTextView.text = user.phoneNum ?: ""
            dateJoinedTextView.text = user.dateJoined

            if (profile.businessName != null) {
                businessNameTextView.visibility = View.VISIBLE
                businessNameTextView.text = profile.businessName
            } else {
                businessNameTextView.visibility = View.GONE
            }

            if (!profile.profileImageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profile.profileImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(100, 100)
                    .placeholder(R.drawable.profile_circle)
                    .error(R.drawable.profile_circle)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.profile_circle)
            }
        }

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<ImageView>(R.id.edit_profile_btn).setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            editProfileLauncher.launch(intent)
        }

        findViewById<Button>(R.id.change_password).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<Button>(R.id.delete_account).setOnClickListener {
            startActivity(Intent(this, DeleteAccountActivity::class.java))
        }
    }

    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            viewModel.fetchUserData()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchUserData()
    }
}
