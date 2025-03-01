package com.ucb.eldroid.farmnook.views.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.databinding.ActivityRegisterBinding
import com.ucb.eldroid.farmnook.model.data.User
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        binding.registerBtn.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // ✅ Get user type from RadioGroup
            val userType = when (binding.rgUserType.checkedRadioButtonId) {
                R.id.rb_user -> "Farmer"
                R.id.rb_hauler -> "Hauler"
                else -> ""
            }

            val phoneNum = binding.phoneNumber.text.toString().trim()

            if (firstName.isNotEmpty() && lastName.isNotEmpty() && email.isNotEmpty() &&
                password.isNotEmpty() && confirmPass.isNotEmpty() &&
                userType.isNotEmpty() && phoneNum.isNotEmpty()
            ) {
                if (password == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = firebaseAuth.currentUser?.uid
                                if (userId != null) {
                                    val currentDate = getCurrentDate() // ✅ Get current date
                                    val user = User(
                                        userId, firstName, lastName, email, password, confirmPass, userType, phoneNum, currentDate, null
                                    )
                                    uploadUserData(this, user)
                                }
                            } else {
                                val errorMessage = task.exception?.message
                                when {
                                    errorMessage?.contains("badly formatted", ignoreCase = true) == true ->
                                        Toast.makeText(this, "Invalid email format!", Toast.LENGTH_SHORT).show()
                                    errorMessage?.contains("email address is already in use", ignoreCase = true) == true ->
                                        Toast.makeText(this, "Email is already registered!", Toast.LENGTH_SHORT).show()
                                    else ->
                                        Toast.makeText(this, "Registration failed: $errorMessage", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
            }
        }


        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

    }

    // ✅ Function to get current date
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    // ✅ Moved uploadUserData outside onCreate and removed unnecessary parts
    private fun uploadUserData(context: Context, user: User) {
        val hashedPassword = hashPassword(user.pass)
        val userRef = database.collection("users").document(user.userId)  // ✅ Create "users" collection

        userRef.set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "User registered successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}
