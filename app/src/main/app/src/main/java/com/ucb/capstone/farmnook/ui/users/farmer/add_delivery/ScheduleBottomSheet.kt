package com.ucb.capstone.farmnook.ui.users.farmer.add_delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.NumberPicker
import android.widget.TextView
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
        val calendarToggle = view.findViewById<TextView>(R.id.calendarToggleButton)
        val calendarContainer = view.findViewById<View>(R.id.calendarContainer)
        val calendarView = view.findViewById<CalendarView>(R.id.calendarView)

        val now = Calendar.getInstance()
        now.add(Calendar.MINUTE, prepMinutes)

        // Initialize CalendarView with correct bounds
        val minCal = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxCal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        calendarView.minDate = minCal.timeInMillis
        calendarView.maxDate = maxCal.timeInMillis

        // Initialize year picker
        yearPicker.minValue = 2025
        yearPicker.maxValue = 2026
        yearPicker.value = 2025

        // Initialize month picker
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = months
        monthPicker.value = now.get(Calendar.MONTH)

        // Initialize day picker
        updateDays(yearPicker.value, monthPicker.value, dayPicker)
        dayPicker.value = now.get(Calendar.DAY_OF_MONTH)

        // Initialize hour picker
        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        hourPicker.value = when (val hour = now.get(Calendar.HOUR)) {
            0 -> 12
            else -> hour
        }

        // Initialize minute picker with formatter for leading zeros
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.setFormatter { i -> String.format("%02d", i) }
        minutePicker.value = now.get(Calendar.MINUTE)

        // Initialize AM/PM picker
        ampmPicker.minValue = 0
        ampmPicker.maxValue = 1
        ampmPicker.displayedValues = arrayOf("AM", "PM")
        ampmPicker.value = if (now.get(Calendar.AM_PM) == Calendar.AM) 0 else 1

        // Calendar toggle and selection handling
        calendarToggle.setOnClickListener {
            if (calendarContainer.visibility == View.VISIBLE) {
                hideCalendar(calendarContainer)
            } else {
                showCalendar(calendarContainer, calendarView, yearPicker, monthPicker, dayPicker)
            }
        }

        // Calendar date selection handling
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            yearPicker.value = year
            monthPicker.value = month
            updateDays(year, month, dayPicker)
            dayPicker.value = dayOfMonth
            hideCalendar(calendarContainer)
        }

        // Update days when month or year changes
        monthPicker.setOnValueChangedListener { _, _, newVal ->
            updateDays(yearPicker.value, newVal, dayPicker)
        }
        yearPicker.setOnValueChangedListener { _, _, newVal ->
            updateDays(newVal, monthPicker.value, dayPicker)
        }

        // Confirm button click handling
        view.findViewById<Button>(R.id.confirmButton).setOnClickListener {
            handleConfirmButton(
                yearPicker.value,
                monthPicker.value,
                dayPicker.value,
                hourPicker.value,
                minutePicker.value,
                ampmPicker.value == 1
            )
        }

        return view
    }

    private fun showCalendar(
        container: View,
        calendarView: CalendarView,
        yearPicker: NumberPicker,
        monthPicker: NumberPicker,
        dayPicker: NumberPicker
    ) {
        // Set calendar to current selected date
        val selectedCal = Calendar.getInstance().apply {
            set(yearPicker.value, monthPicker.value, dayPicker.value)
        }
        calendarView.date = selectedCal.timeInMillis

        // Show calendar with animation
        container.visibility = View.VISIBLE
        container.alpha = 0f
        container.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideCalendar(container: View) {
        container.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                container.visibility = View.GONE
            }
            .start()
    }

    private fun handleConfirmButton(year: Int, month: Int, day: Int, hour: Int, minute: Int, isPM: Boolean) {
        var adjustedHour = hour
        if (hour == 12) adjustedHour = 0
        if (isPM) adjustedHour += 12

        val selectedCal = Calendar.getInstance().apply {
            set(year, month, day, adjustedHour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calculate minimum allowed datetime
        val minAllowed = Calendar.getInstance().apply {
            add(Calendar.MINUTE, prepMinutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Check if selected time is before allowed time
        if (selectedCal.before(minAllowed)) {
            Toast.makeText(
                requireContext(),
                "Please select a schedule beyond the preparation time.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val scheduleTimestamp = Timestamp(selectedCal.time)
        onScheduleSelected(scheduleTimestamp)
        dismiss()
    }

    private fun updateDays(year: Int, month: Int, dayPicker: NumberPicker) {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.minValue = 1
        dayPicker.maxValue = maxDay
        if (dayPicker.value > maxDay) dayPicker.value = maxDay
    }
} 