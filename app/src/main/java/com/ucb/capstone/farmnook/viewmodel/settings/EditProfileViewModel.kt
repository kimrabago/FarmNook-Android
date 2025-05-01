package com.ucb.capstone.farmnook.ui.settings

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.ucb.capstone.farmnook.data.model.UserProfile

class EditProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile

    private val _profileImageUri = MutableLiveData<String?>()
    val profileImageUri: LiveData<String?> = _profileImageUri

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _showLocation = MutableLiveData<Boolean>()
    val showLocation: LiveData<Boolean> = _showLocation

    fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get(Source.CACHE)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userProfile = UserProfile(
                        user = com.ucb.capstone.farmnook.data.model.User(
                            userId = userId,
                            firstName = document.getString("firstName") ?: "",
                            lastName = document.getString("lastName") ?: "",
                            email = document.getString("email") ?: "",
                            userType = document.getString("userType") ?: "",
                            phoneNum = document.getString("phoneNum"),
                            dateJoined = document.getString("dateJoined") ?: ""
                        ),
                        profileImageUrl = document.getString("profileImageUrl"),
                        businessId = document.getString("businessId"),
                        businessName = document.getString("businessName")
                    )

                    _userProfile.value = userProfile

                    val userType = document.getString("userType") ?: ""
                    _showLocation.value = userType == "Hauler Business Admin"
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun uploadProfileImage(uri: Uri, fileName: String) {
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storage.child("profileImages/$userId/$fileName")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    db.collection("users").document(userId)
                        .update("profileImageUrl", downloadUri.toString())
                        .addOnSuccessListener {
                            _profileImageUri.value = downloadUri.toString()
                        }
                        .addOnFailureListener { it.printStackTrace() }
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun saveProfileData(
        firstName: String,
        lastName: String,
        email: String,
        phoneNum: String,
        businessName: String?,
        showBusinessName: Boolean
    ) {
        val userId = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "phoneNum" to phoneNum
        )
        if (showBusinessName) {
            updates["businessName"] = businessName ?: ""
        }

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener { _updateSuccess.value = true }
            .addOnFailureListener {
                it.printStackTrace()
                _updateSuccess.value = false
            }
    }
}
