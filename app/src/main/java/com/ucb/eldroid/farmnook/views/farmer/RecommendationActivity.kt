package com.ucb.eldroid.farmnook.views.farmer

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ucb.eldroid.farmnook.R
import com.ucb.eldroid.farmnook.model.data.Hauler
import com.ucb.eldroid.farmnook.views.adapter.HaulerAdapter

class RecommendationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish() // Closes activity
        }
    }
}
