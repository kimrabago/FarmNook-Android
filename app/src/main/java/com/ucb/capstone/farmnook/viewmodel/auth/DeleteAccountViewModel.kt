package com.ucb.capstone.farmnook.viewmodel.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeleteAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> get() = _deleteSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun reAuthenticateAndDelete(password: String) {
        val user = auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    deleteUserAccount()
                }
                .addOnFailureListener { e ->
                    _errorMessage.value = "Invalid password. Please try again."
                }
        } else {
            _errorMessage.value = "User not authenticated."
        }
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        val userID = user?.uid

        if (userID != null) {
            firestore.collection("users").document(userID)
                .delete()
                .addOnSuccessListener {
                    user.delete()
                        .addOnSuccessListener {
                            _deleteSuccess.value = true
                        }
                        .addOnFailureListener {
                            _errorMessage.value = "Account deletion failed. Please try again."
                        }
                }
                .addOnFailureListener {
                    _errorMessage.value = "Failed to delete account data. Try again."
                }
        } else {
            _errorMessage.value = "User ID not found."
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}