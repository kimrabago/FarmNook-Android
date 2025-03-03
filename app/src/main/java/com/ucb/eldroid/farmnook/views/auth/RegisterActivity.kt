package com.ucb.eldroid.farmnook.views.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.databinding.ActivityRegisterBinding
import com.ucb.eldroid.farmnook.model.data.User
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private val handler = Handler(Looper.getMainLooper())

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        binding.registerBtn.setOnClickListener {
            registerUser()
        }



        binding.signInHere.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        setupPasswordToggle(binding.etPassword)
        setupPasswordToggle(binding.etConfirmPassword)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawablesRelative[2] // Get drawableEnd (eye icon)
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width() - editText.paddingEnd)) {
                    togglePasswordVisibility(editText)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        val isCurrentlyVisible = if (editText.id == R.id.et_password) isPasswordVisible else isConfirmPasswordVisible

        if (isCurrentlyVisible) {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_closed, 0)
        } else {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.pass_icon, 0, R.drawable.eye_open, 0)
        }

        if (editText.id == R.id.et_password) {
            isPasswordVisible = !isPasswordVisible
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        editText.setSelection(editText.text.length) // Keep cursor at the end
    }

    private fun registerUser() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()
        val phoneNum = binding.phoneNumber.text.toString().trim()

        val userType = when (binding.rgUserType.checkedRadioButtonId) {
            R.id.rb_user -> "Farmer"
            R.id.rb_hauler -> "Hauler"
            else -> ""
        }

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPass.isEmpty() || phoneNum.isEmpty() || userType.isEmpty()) {
            showToast("Please fill in all fields!")
            return
        }

        if (password != confirmPass) {
            showToast("Passwords do not match!")
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        sendEmailVerification(it) // 🔹 Send email verification
                        val hashedPassword = hashPassword(password)
                        val userData = User(
                            user.uid, firstName, lastName, email, hashedPassword, userType, phoneNum, getCurrentDate(), null
                        )
                        saveUserToFirestore(this, userData)
                        startEmailVerificationCheck() // 🔹 Start checking for email verification
                    }
                } else {
                    handleAuthError(task.exception?.message)
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Verification email sent! Please check your inbox.")
                } else {
                    showToast("Failed to send verification email.")
                }
            }
    }

    private fun startEmailVerificationCheck() {
        val runnable = object : Runnable {
            override fun run() {
                firebaseAuth.currentUser?.reload()?.addOnCompleteListener {
                    if (firebaseAuth.currentUser?.isEmailVerified == true) {
                        firebaseAuth.signOut()
                        showToast("Email verified! Redirecting to login...")
                        navigateToLogin()
                    } else {
                        handler.postDelayed(this, 3000) // 🔁 Check again in 3 seconds
                    }
                }
            }
        }
        handler.post(runnable)
    }

    private fun saveUserToFirestore(context: Context, user: User) {
        database.collection("users").document(user.userId)
            .set(user)
            .addOnSuccessListener {
                startEmailVerificationCheck()
            }
            .addOnFailureListener { e ->
                showToast("Failed to save user data: ${e.message}")
            }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleAuthError(errorMessage: String?) {
        when {
            errorMessage?.contains("badly formatted", ignoreCase = true) == true ->
                showToast("Invalid email format!")
            errorMessage?.contains("email address is already in use", ignoreCase = true) == true ->
                showToast("Email is already registered!")
            else ->
                showToast("Registration failed: $errorMessage")
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}