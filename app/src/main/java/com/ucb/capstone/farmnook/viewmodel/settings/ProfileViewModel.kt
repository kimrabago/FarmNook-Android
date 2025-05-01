package com.ucb.capstone.farmnook.viewmodel.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.ucb.capstone.farmnook.data.model.User
import com.ucb.capstone.farmnook.data.model.UserProfile

class ProfileViewModel : ViewModel() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> get() = _userProfile

    fun fetchUserData() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        database.collection("users").document(userId).get(Source.CACHE)
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = User(
                        userId = userId,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        email = document.getString("email") ?: "",
                        phoneNum = document.getString("phoneNum"),
                        userType = document.getString("userType") ?: "",
                        dateJoined = document.getString("dateJoined") ?: ""
                    )

                    val profileImageUrl = document.getString("profileImageUrl")
                    val businessId = document.getString("businessId")
                    val userType = user.userType

                    if (userType == "Hauler" || userType == "Hauler Business Admin") {
                        val businessDocId = if (userType == "Hauler") businessId else userId
                        if (!businessDocId.isNullOrEmpty()) {
                            database.collection("users").document(businessDocId).get(Source.CACHE)
                                .addOnSuccessListener { businessDoc ->
                                    val businessName = businessDoc.getString("businessName") ?: "N/A"
                                    _userProfile.postValue(
                                        UserProfile(user, profileImageUrl, businessId, businessName)
                                    )
                                }
                        } else {
                            _userProfile.postValue(
                                UserProfile(user, profileImageUrl, businessId, null)
                            )
                        }
                    } else {
                        _userProfile.postValue(
                            UserProfile(user, profileImageUrl, null, null)
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileViewModel", "Failed to fetch user data", e)
            }
    }
}