package com.ucb.eldroid.farmnook.views.settings

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.ucb.eldroid.farmnook.R
import java.util.Calendar

class HaulerProfileActivity : AppCompatActivity() {

    private lateinit var calendarEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hauler_profile)

        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        val editProfileButton = findViewById<Button>(R.id.edit_profile_btn)
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Setup the real-time calendar for the "Date Joined" field
        calendarEditText = findViewById(R.id.calendar)
        // Disable keyboard input for this field
        calendarEditText.inputType = InputType.TYPE_NULL

        // Set the current date as default
        val currentCalendar = Calendar.getInstance()
        updateDateInView(currentCalendar)

        // Open DatePickerDialog when the EditText is clicked
        calendarEditText.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        // Get current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create DatePickerDialog
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            updateDateInView(selectedCalendar)
        }, year, month, day)

        datePickerDialog.show()
    }

    // Update the EditText with the formatted date
    private fun updateDateInView(calendar: Calendar) {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        val year = calendar.get(Calendar.YEAR)
        val formattedDate = "$day/$month/$year"
        calendarEditText.setText(formattedDate)
    }
}
