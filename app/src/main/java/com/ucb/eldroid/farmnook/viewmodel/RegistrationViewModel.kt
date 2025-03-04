package com.ucb.eldroid.farmnook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.model.data.User
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
                        sendEmailVerification(it)
                        val hashedPassword = hashPassword(password)
                        val userData = User(
                            it.uid, firstName, lastName, email, hashedPassword, userType, phoneNum, getCurrentDate(), null
                        )
                        saveUserToFirestore(userData)
                    }
                } else {
                    handleAuthError(task.exception?.message)
                }
            }
    }

    private fun sendEmailVerification(user: FirebaseUser) {
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                _emailVerificationStatus.value = if (task.isSuccessful) {
                    "Verification email sent! Please check your inbox."
                } else {
                    "Failed to send verification email."
                }
            }
    }

    private fun saveUserToFirestore(user: User) {
        database.collection("users").document(user.userId)
            .set(user)
            .addOnSuccessListener {
                _registrationStatus.value = "Registration successful! Please verify your email."
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