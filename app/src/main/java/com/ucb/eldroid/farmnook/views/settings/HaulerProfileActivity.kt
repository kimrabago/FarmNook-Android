package com.ucb.eldroid.farmnook.views.settings

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.views.auth.ChangePasswordActivity
import com.ucb.eldroid.farmnook.views.auth.DeleteAccountActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HaulerProfileActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hauler_profile)

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

                        val fullName = "$firstName $lastName"
                        // ✅ Get dateJoined and format it
                        val dateJoined = document.getString("dateJoined") ?: ""

                        // ✅ Update the UI in hauler_profile layout
                        val fullNameTextView : TextView = findViewById(R.id.fullName)
                        val emailTextView : TextView = findViewById(R.id.email)
                        val phoneNumTextView : TextView = findViewById(R.id.phone_num)
                        val dateJoinedTextView : TextView = findViewById(R.id.dateJoined)


                        fullNameTextView.text = fullName
                        emailTextView.text = email
                        phoneNumTextView.text = phoneNumber
                        dateJoinedTextView.text  = dateJoined
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

}
