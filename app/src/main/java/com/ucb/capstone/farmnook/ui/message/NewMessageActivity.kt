package com.ucb.capstone.farmnook.ui.message

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.User
import com.ucb.capstone.farmnook.ui.message.ContactAdapter

class NewMessageActivity : AppCompatActivity() {

    private lateinit var searchContact: AutoCompleteTextView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val nameList = mutableListOf<String>()
    private val userIdMap = mutableMapOf<String, String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var recyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MessagePrefs", Context.MODE_PRIVATE)

        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        searchContact = findViewById(R.id.searchContact)
        recyclerView = findViewById(R.id.messagesRecyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        contactAdapter = ContactAdapter(nameList) { userName ->
            // Handle contact selection
            val selectedUserId = userIdMap[userName]
            selectedUserId?.let {
                openChatScreen(it, userName)
            }
        }
        recyclerView.adapter = contactAdapter

        // Fetch current user details
        fetchCurrentUser()

        // Load recent search names
        loadRecentNames()

        // Handle search input changes
        searchContact.addTextChangedListener {
            val query = it.toString()
            if (query.isNotEmpty()) {
                val filteredNames = nameList.filter { name ->
                    name.contains(query, ignoreCase = true)
                }
                contactAdapter.updateList(filteredNames)
            } else {
                contactAdapter.updateList(nameList) // Show all when no search input
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
        val targetUserTypes = if (currentUserType == "Farmer") {
            listOf("Hauler", "Hauler Business Admin")
        } else {
            listOf("Farmer")
        }

        firestore.collection("users")
            .whereIn("userType", targetUserTypes)
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

                // Notify the adapter
                contactAdapter.updateList(nameList)

                // Save recent search names in SharedPreferences
                saveRecentNames()
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

    private fun saveRecentNames() {
        // Convert name list to a string and save to SharedPreferences
        val recentNames = nameList.joinToString(",")
        val editor = sharedPreferences.edit()
        editor.putString("recentNames", recentNames)
        editor.apply()
    }

    private fun loadRecentNames() {
        // Load saved names from SharedPreferences
        val recentNames = sharedPreferences.getString("recentNames", "")
        if (!recentNames.isNullOrEmpty()) {
            val names = recentNames.split(",")
            nameList.addAll(names)
            contactAdapter.updateList(nameList)
        }
    }
}
