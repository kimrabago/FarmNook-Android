package com.ucb.eldroid.farmnook.views.message

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.User

class NewMessageActivity : AppCompatActivity() {

    private lateinit var searchContact: AutoCompleteTextView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val nameList = mutableListOf<String>()
    private val userIdMap = mutableMapOf<String, String>() // Store name-to-UID mapping
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish() // Closes the activity and goes back
        }

        searchContact = findViewById(R.id.searchContact)

        adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nameList)
        searchContact.setAdapter(adapter)

        fetchCurrentUser()

        // Handle item click
        searchContact.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position) ?: return@setOnItemClickListener
            val selectedUserId = userIdMap[selectedName]

            if (selectedUserId != null) {
                openChatScreen(selectedUserId, selectedName)
            } else {
                Toast.makeText(this, "User ID not found!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchCurrentUser() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentUser = document.toObject(User::class.java)
                        currentUser?.let { fetchUsers(it.userType) }
                    } else {
                        Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchUsers(currentUserType: String) {
        val targetUserType = if (currentUserType == "Hauler") "Farmer" else "Hauler"

        firestore.collection("users")
            .whereEqualTo("userType", targetUserType)
            .get()
            .addOnSuccessListener { documents ->
                nameList.clear()
                userIdMap.clear()

                for (doc in documents) {
                    val user = doc.toObject(User::class.java)
                    val fullName = "${user.firstName} ${user.lastName}"
                    nameList.add(fullName)
                    userIdMap[fullName] = user.userId
                }

                if (nameList.isEmpty()) {
                    Toast.makeText(this, "No users found!", Toast.LENGTH_SHORT).show()
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChatScreen(userId: String, userName: String) {
        val intent = Intent(this, MessageActivity::class.java)
        intent.putExtra("receiverId", userId)
        intent.putExtra("receiverName", userName)
        startActivity(intent)
    }
}
