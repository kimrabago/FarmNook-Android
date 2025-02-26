package com.ucb.eldroid.farmnook.views.auth

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R

class DeleteAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure that your layout file for deleting account is named "activity_delete_account.xml"
        setContentView(R.layout.activity_delete_account)

        // Button to confirm deletion (assign an ID in your XML, e.g., btn_confirm_delete)
        val confirmDeleteButton = findViewById<Button>(R.id.btn_confirm_delete)
        confirmDeleteButton.setOnClickListener {
            // TODO: Implement the account deletion logic (e.g., call your API)
        }

        // Button to cancel deletion (assign an ID in your XML, e.g., btn_cancel_delete)
        val cancelButton = findViewById<Button>(R.id.btn_cancel_delete)
        cancelButton.setOnClickListener {
            finish() // Cancel deletion and return to the previous screen
        }
    }
}
