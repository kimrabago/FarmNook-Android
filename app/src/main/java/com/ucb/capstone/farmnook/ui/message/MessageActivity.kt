package com.ucb.capstone.farmnook.ui.message

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.ui.adapter.MessageAdapter

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



class MessageActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var replyEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var receiverNameTextView: TextView

    private lateinit var messageAdapter: MessageAdapter
    private val viewModel: MessageViewModel by viewModels()

    private lateinit var chatId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        replyEditText = findViewById(R.id.replyEditText)
        sendButton = findViewById(R.id.sendButton)
        receiverNameTextView = findViewById(R.id.receiverName)

        chatId = intent.getStringExtra("chatId") ?: run {
            Toast.makeText(this, "Chat not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        receiverId = intent.getStringExtra("recipientId") ?: run {
            Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener { finish() }

        receiverName = intent.getStringExtra("receiverName") ?: "Unknown User"
        receiverNameTextView.text = receiverName


        viewModel.initialize(chatId, receiverId)


        messageAdapter = MessageAdapter(emptyList(), viewModel.senderId)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageActivity)
            adapter = messageAdapter
        }


        observeViewModel()

        sendButton.setOnClickListener {
            val messageText = replyEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                viewModel.sendMessage(messageText)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                messageAdapter.updateMessages(messages)
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.messageSent.observe(this) { sent ->
            if (sent) {
                replyEditText.text.clear()
                viewModel.resetMessageSent()
            }
        }
    }
}