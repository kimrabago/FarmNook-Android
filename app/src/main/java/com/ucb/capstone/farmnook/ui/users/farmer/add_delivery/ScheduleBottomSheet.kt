package com.ucb.capstone.farmnook.ui.users.farmer.add_delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.ucb.capstone.farmnook.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleBottomSheet(
    private val prepMinutes: Int,
    private val onScheduleSelected: (Timestamp) -> Unit
) : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_schedule, container, false)
        val yearPicker = view.findViewById<NumberPicker>(R.id.yearPicker)
        val monthPicker = view.findViewById<NumberPicker>(R.id.monthPicker)
        val dayPicker = view.findViewById<NumberPicker>(R.id.dayPicker)
        val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = view.findViewById<NumberPicker>(R.id.minutePicker)
        val ampmPicker = view.findViewById<NumberPicker>(R.id.ampmPicker)

        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, prepMinutes)

        val currentYear = now.get(Calendar.YEAR)
        yearPicker.minValue = currentYear
        yearPicker.maxValue = currentYear + 1
        yearPicker.value = currentYear

        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = now.get(Calendar.MONTH)

        updateDays(currentYear, monthPicker.value, dayPicker)
        dayPicker.value = now.get(Calendar.DAY_OF_MONTH)

        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        hourPicker.value = when (val hour = now.get(Calendar.HOUR)) {
            0 -> 12
            else -> hour
        }

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.setFormatter { i -> String.format("%02d", i) }
        minutePicker.value = now.get(Calendar.MINUTE)

        ampmPicker.minValue = 0
        ampmPicker.maxValue = 1
        ampmPicker.displayedValues = arrayOf("AM", "PM")
        ampmPicker.value = if (now.get(Calendar.AM_PM) == Calendar.AM) 0 else 1

        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            val year = yearPicker.value
            val month = monthPicker.value
            val day = dayPicker.value
            var hour = hourPicker.value
            val minute = minutePicker.value
            val isPM = ampmPicker.value == 1

            if (hour == 12) hour = 0
            if (isPM) hour += 12

            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, month, day, hour, minute)

            // Calculate minimum allowed datetime
            val minAllowed = Calendar.getInstance()
            minAllowed.add(Calendar.MINUTE, prepMinutes)

            // 🛑 Check if selected time is before allowed time
            if (selectedCal.before(minAllowed)) {
                Toast.makeText(requireContext(), "Please select a schedule beyond the preparation time.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val scheduleTimestamp = Timestamp(selectedCal.time)
            onScheduleSelected(scheduleTimestamp)
            dismiss()
        }

        return view
    }

    fun updateDays(year: Int, month: Int, dayPicker: NumberPicker) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.minValue = 1
        dayPicker.maxValue = maxDay
        if (dayPicker.value > maxDay) dayPicker.value = maxDay
    }
}
