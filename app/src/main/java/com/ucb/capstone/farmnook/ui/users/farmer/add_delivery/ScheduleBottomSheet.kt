package com.ucb.capstone.farmnook.ui.users.farmer.add_delivery

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.ucb.capstone.farmnook.R
import com.ucb.capstone.farmnook.data.model.DeliveryRequest
import com.ucb.capstone.farmnook.data.model.ScheduledDelivery
import com.ucb.capstone.farmnook.ui.adapter.ScheduledDeliveryAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ScheduleBottomSheet(
    private val prepMinutes: Int,
    private val bookedTimes: List<Timestamp>,
    private val vehicleId: String,
    private val onScheduleSelected: (Timestamp) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var yearPicker: NumberPicker
    private lateinit var monthPicker: NumberPicker
    private lateinit var dayPicker: NumberPicker
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var ampmPicker: NumberPicker
    private lateinit var calendarToggle: TextView
    private lateinit var confirmButton: Button
    private lateinit var scheduledListRecyclerView: RecyclerView
    private lateinit var scheduledDeliveryAdapter: ScheduledDeliveryAdapter
    private val deliveryList = mutableListOf<ScheduledDelivery>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("DEBUG_INIT_BOTTOM_SHEET", "ScheduleBottomSheet opened with vehicleId=$vehicleId")

        val view = inflater.inflate(R.layout.bottom_sheet_schedule, container, false)
        bindViews(view)
        setupCalendarView()
        setupPickers()
        setupListeners()
        setupRecyclerView()

        val today = Calendar.getInstance()
        loadScheduledDeliveriesForDate(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )
        return view
    }

    private fun bindViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        yearPicker = view.findViewById(R.id.yearPicker)
        monthPicker = view.findViewById(R.id.monthPicker)
        dayPicker = view.findViewById(R.id.dayPicker)
        hourPicker = view.findViewById(R.id.hourPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
        ampmPicker = view.findViewById(R.id.ampmPicker)
        calendarToggle = view.findViewById(R.id.calendarToggleButton)
        confirmButton = view.findViewById(R.id.confirmButton)
        scheduledListRecyclerView = view.findViewById(R.id.CurrentDeliveriesRecyclerView)
    }

    private fun setupCalendarView() {
        calendarView.setFocusedMonthDateColor(ContextCompat.getColor(requireContext(), R.color.dark_green))

        val minCal = Calendar.getInstance().apply { set(2025, Calendar.JANUARY, 1) }
        val maxCal = Calendar.getInstance().apply { set(2026, Calendar.DECEMBER, 31) }

        calendarView.minDate = minCal.timeInMillis
        calendarView.maxDate = maxCal.timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            yearPicker.value = year
            monthPicker.value = month
            updateDays(year, month, dayPicker)
            dayPicker.value = dayOfMonth
            loadScheduledDeliveriesForDate(year, month, dayOfMonth)
            Log.d("DEBUG_CALENDAR_CLICK", "Calendar date selected: $year-${month + 1}-$dayOfMonth")

        }
    }

    private fun setupPickers() {
        val now = Calendar.getInstance().apply {
            add(Calendar.MINUTE, prepMinutes)
        }

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

        val minuteOptions = arrayOf("00", "30")
        minutePicker.minValue = 0
        minutePicker.maxValue = minuteOptions.lastIndex
        minutePicker.displayedValues = minuteOptions
        minutePicker.value = if (now.get(Calendar.MINUTE) >= 30) 1 else 0

        ampmPicker.minValue = 0
        ampmPicker.maxValue = 1
        ampmPicker.displayedValues = arrayOf("AM", "PM")
        ampmPicker.value = if (now.get(Calendar.AM_PM) == Calendar.AM) 0 else 1

        monthPicker.setOnValueChangedListener { _, _, newVal ->
            updateDays(yearPicker.value, newVal, dayPicker)
        }

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            updateDays(newVal, monthPicker.value, dayPicker)
        }
    }

    private fun setupListeners() {
        calendarToggle.setOnClickListener {
            calendarView.visibility = if (calendarView.visibility == View.VISIBLE) View.GONE else View.VISIBLE

            val selectedCal = Calendar.getInstance().apply {
                set(yearPicker.value, monthPicker.value, dayPicker.value)
            }
            calendarView.date = selectedCal.timeInMillis
        }

        confirmButton.setOnClickListener {
            val selectedTimestamp = buildTimestampFromPickers()
            val selectedDateTime = selectedTimestamp.toDate()

            val minAllowed = Calendar.getInstance().apply {
                add(Calendar.MINUTE, prepMinutes)
            }

            val isAlreadyBooked = bookedTimes.any {
                val booked = it.toDate()
                booked.year == selectedDateTime.year &&
                        booked.month == selectedDateTime.month &&
                        booked.date == selectedDateTime.date &&
                        booked.hours == selectedDateTime.hours &&
                        booked.minutes == selectedDateTime.minutes
            }

            if (isAlreadyBooked) {
                Toast.makeText(requireContext(), "This slot is already booked.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDateTime.before(minAllowed.time)) {
                Toast.makeText(
                    requireContext(),
                    "Please select a schedule beyond the preparation time.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            onScheduleSelected(selectedTimestamp)
            dismiss()
        }
    }

    private fun buildTimestampFromPickers(): Timestamp {
        val year = yearPicker.value
        val month = monthPicker.value
        val day = dayPicker.value
        var hour = hourPicker.value
        val minute = if (minutePicker.value == 0) 0 else 30
        val isPM = ampmPicker.value == 1

        if (hour == 12) hour = 0
        if (isPM) hour += 12

        val selectedCal = Calendar.getInstance()
        selectedCal.set(year, month, day, hour, minute)
        return Timestamp(selectedCal.time)
    }

    private fun updateDays(year: Int, month: Int, dayPicker: NumberPicker) {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        dayPicker.minValue = 1
        dayPicker.maxValue = maxDay
        if (dayPicker.value > maxDay) dayPicker.value = maxDay
    }

    private fun setupRecyclerView() {
        scheduledDeliveryAdapter = ScheduledDeliveryAdapter(deliveryList)
        scheduledListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        scheduledListRecyclerView.adapter = scheduledDeliveryAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadScheduledDeliveriesForDate(year: Int, month: Int, day: Int) {
        val calendarStart = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calendarEnd = Calendar.getInstance().apply {
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        Log.d("DEBUG_FETCH_DATE", "Loading deliveries for vehicleId=$vehicleId on $year-${month + 1}-$day")
        Log.d("DEBUG_FETCH_DATE", "Start=${Timestamp(calendarStart.time)}, End=${Timestamp(calendarEnd.time)}")

        val emptyView = view?.findViewById<TextView>(R.id.emptyView)
        val note = view?.findViewById<TextView>(R.id.note)

        FirebaseFirestore.getInstance()
            .collection("deliveries")
            .whereEqualTo("vehicleId", vehicleId)
            .whereGreaterThanOrEqualTo("scheduledTime", Timestamp(calendarStart.time))
            .whereLessThanOrEqualTo("scheduledTime", Timestamp(calendarEnd.time))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("DEBUG_DELIVERY_FETCH", "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    deliveryList.clear()
                    emptyView?.visibility = View.VISIBLE
                    note?.visibility = View.GONE
                    scheduledListRecyclerView.visibility = View.GONE
                    scheduledDeliveryAdapter.notifyDataSetChanged()
                    return@addSnapshotListener
                } else {
                    note?.visibility = View.VISIBLE
                }

                deliveryList.clear()
                var processedCount = 0
                val documents = snapshot.documents

                for (doc in documents) {
                    val requestId = doc.getString("requestId") ?: continue
                    val scheduledTime = doc.getTimestamp("scheduledTime") ?: continue

                    FirebaseFirestore.getInstance()
                        .collection("deliveryRequests")
                        .document(requestId)
                        .get()
                        .addOnSuccessListener { requestDoc ->
                            val overallEst = requestDoc.getLong("overallEstimatedTime")?.toInt() ?: 0
                            Log.d("DEBUG_CALENDAR_CLICK", "overallEstimatedTime: $overallEst")

                            deliveryList.add(
                                ScheduledDelivery(
                                    scheduledTime = scheduledTime,
                                    overallEstimatedTime = overallEst
                                )
                            )
                            Log.d("DEBUG_CALENDAR_CLICK", "Added to deliveryList: scheduledTime=$scheduledTime, overallEstimatedTime=$overallEst")
                        }
                        .addOnCompleteListener {
                            processedCount++
                            if (processedCount == documents.size) {
                                emptyView?.visibility = View.GONE
                                note?.visibility = View.GONE
                                scheduledListRecyclerView.visibility = View.VISIBLE
                                scheduledDeliveryAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
    }
}