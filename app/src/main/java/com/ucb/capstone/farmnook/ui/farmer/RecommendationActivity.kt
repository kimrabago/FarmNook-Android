package com.ucb.capstone.farmnook.ui.farmer

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.ucb.capstone.farmnook.R

class RecommendationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish() // Closes activity
        }
    }
}
