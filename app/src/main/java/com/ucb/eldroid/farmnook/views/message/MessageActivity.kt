package com.ucb.eldroid.farmnook.views.message

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R

class MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        // Set up the back button to finish the activity
        val btnBack: ImageButton = findViewById(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        // Retrieve and set the receiver's name and the initial message from the intent
        val receiverNameTextView: TextView = findViewById(R.id.receiverName)
        val senderName = intent.getStringExtra("SENDER_NAME") ?: "Receiver"
        val initialMessage = intent.getStringExtra("MESSAGE_CONTENT") ?: ""
        receiverNameTextView.text = senderName

        // Get references to our message container views
        val repliesContainer: LinearLayout = findViewById(R.id.repliesContainer)
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        val replyEditText: EditText = findViewById(R.id.replyEditText)
        val sendButton: Button = findViewById(R.id.sendButton)

        // Pre-populate the conversation container with the initial message (if any)
        if (initialMessage.isNotEmpty()) {
            val initialMessageTextView = TextView(this).apply {
                text = initialMessage
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            repliesContainer.addView(initialMessageTextView)
        }

        // Set up the send button to add new messages to the conversation container
        sendButton.setOnClickListener {
            val messageText = replyEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                // Create a new TextView for the sent message
                val newMessageTextView = TextView(this).apply {
                    text = messageText
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                }
                // Add the new message to the replies container
                repliesContainer.addView(newMessageTextView)
                // Clear the input field
                replyEditText.text.clear()
                // Scroll to the bottom to show the new message
                scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            }
        }
    }
}
