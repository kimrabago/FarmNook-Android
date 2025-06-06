package com.ucb.capstone.farmnook.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _loginResult = MutableLiveData<Result<FirebaseUser?>>()
    val loginResult: LiveData<Result<FirebaseUser?>> get() = _loginResult

    private val _googleSignInResult = MutableLiveData<Result<FirebaseUser?>>()
    val googleSignInResult: LiveData<Result<FirebaseUser?>> get() = _googleSignInResult

    fun loginWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _loginResult.value = Result.success(firebaseAuth.currentUser)
                } else {
                    _loginResult.value = Result.failure(task.exception ?: Exception("Login failed"))
                }
            }
    }

    fun authenticateWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _googleSignInResult.value = Result.success(firebaseAuth.currentUser)
                } else {
                    _googleSignInResult.value = Result.failure(task.exception ?: Exception("Google sign-in failed"))
                }
            }
    }

    fun getUserData(user: FirebaseUser?, onResult: (String?, String?, String?, String?) -> Unit) {
        user?.let {
            val userRef = database.collection("users").document(it.uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName")
                        val lastName = document.getString("lastName")
                        val email = document.getString("email")
                        val userType = document.getString("userType") ?: "farmer" // Default to farmer

                        onResult(firstName, lastName, email, userType)
                    } else {
                        onResult(null, null, null, null)
                    }
                }
                .addOnFailureListener {
                    onResult(null, null, null, null)
                }
        } ?: onResult(null, null, null, null)
    }
}