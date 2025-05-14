package com.ucb.capstone.farmnook.utils

object CombineTimeDurations {
    // This method parses a time string like "1 hr 30 min" or "25 min" and returns total minutes
    fun parseMinutes(time: String): Int {
        val hourRegex = Regex("(\\d+)\\s*hr")
        val minRegex = Regex("(\\d+)\\s*min")

        val hours = hourRegex.find(time)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = minRegex.find(time)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return (hours * 60) + minutes
    }

    // This method combines two duration strings into a single readable format like "1 hr 45 min"
    fun combine(time1: String, time2: String): String {
        val totalMinutes = parseMinutes(time1) + parseMinutes(time2)
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60

        return when {
            hrs > 0 && mins > 0 -> "$hrs hr $mins min"
            hrs > 0 -> "$hrs hr"
            else -> "$mins min"
        }
    }
}