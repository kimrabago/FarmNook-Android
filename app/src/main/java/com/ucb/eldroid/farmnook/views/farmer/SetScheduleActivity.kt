package com.ucb.eldroid.farmnook.views.farmer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ucb.eldroid.farmnook.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SetScheduleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_schedule)

        val dateInput = findViewById<EditText>(R.id.dateInput)
        val timeInput = findViewById<EditText>(R.id.timeInput)
        val setButton = findViewById<Button>(R.id.setButton)

        val calendar = Calendar.getInstance()

        // Date Picker Dialog
        dateInput.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    dateInput.setText(dateFormat.format(selectedDate.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Time Picker Dialog with AM/PM selection
        timeInput.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val amPm = if (hourOfDay < 12) "AM" else "PM"
                    val hour = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour, minute, amPm)
                    timeInput.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // False for AM/PM format
            )
            timePickerDialog.show()
        }

        // Set button click listener
        setButton.setOnClickListener {
            val selectedDate = dateInput.text.toString().trim()
            val selectedTime = timeInput.text.toString().trim()

            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                dateInput.error = "Please select a date"
                timeInput.error = "Please select a time"
            } else {
                // Proceed with saving or using the date/time
            }
        }
    }
}