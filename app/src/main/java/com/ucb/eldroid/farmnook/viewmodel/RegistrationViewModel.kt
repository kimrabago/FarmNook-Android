package com.ucb.eldroid.farmnook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.data.model.User
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrationViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _registrationStatus = MutableLiveData<String>()
    val registrationStatus: LiveData<String> = _registrationStatus

    private val _emailVerificationStatus = MutableLiveData<String>()
    val emailVerificationStatus: LiveData<String> = _emailVerificationStatus

    fun registerUser(firstName: String, lastName: String, email: String, password: String, confirmPass: String, phoneNum: String, userType: String) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPass.isEmpty() || phoneNum.isEmpty() || userType.isEmpty()) {
            _registrationStatus.value = "Please fill in all fields!"
            return
        }

        if (password != confirmPass) {
            _registrationStatus.value = "Passwords do not match!"
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        sendEmailVerification(it, firstName, lastName, phoneNum, userType)
                    }
                } else {
                    handleAuthError(task.exception?.message)
                }
            }
    }


    private fun sendEmailVerification(user: FirebaseUser, firstName: String, lastName: String, phoneNum: String, userType: String) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _emailVerificationStatus.value = "Verification email sent! Please check your inbox."

                    // Pass actual values instead of hardcoded ones
                    checkEmailVerification(user, firstName, lastName, phoneNum, userType)
                } else {
                    _emailVerificationStatus.value = "Failed to send verification email."
                }
            }
    }

    private fun checkEmailVerification(user: FirebaseUser, firstName: String, lastName: String, phoneNum: String, userType: String) {
        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                user.reload()
                if (user.isEmailVerified) {
                    val hashedPassword = hashPassword(user.email ?: "")
                    val userData = User(
                        user.uid,
                        firstName, // Now using actual input
                        lastName,  // Now using actual input
                        user.email ?: "",
                        hashedPassword,
                        userType,  // Now using actual input
                        phoneNum,  // Now using actual input
                        getCurrentDate(),
                        null
                    )

                    saveUserToFirestore(userData)
                } else {
                    handler.postDelayed(this, 3000) // Check again after 3 seconds
                }
            }
        }

        handler.postDelayed(runnable, 3000) // Start checking in 3 seconds
    }


    private fun saveUserToFirestore(user: User) {
        database.collection("users").document(user.userId)
            .set(user)
            .addOnSuccessListener {
                _registrationStatus.value = "Registration successful! You can now log in."
            }
            .addOnFailureListener { e ->
                _registrationStatus.value = "Failed to save user data: ${e.message}"
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

    private fun handleAuthError(errorMessage: String?) {
        when {
            errorMessage?.contains("badly formatted", ignoreCase = true) == true ->
                _registrationStatus.value = "Invalid email format!"
            errorMessage?.contains("email address is already in use", ignoreCase = true) == true ->
                _registrationStatus.value = "Email is already registered!"
            else ->
                _registrationStatus.value = "Registration failed: $errorMessage"
        }
    }
}