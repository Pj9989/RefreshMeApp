package com.refreshme.use_case

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GenerateTimeSlotsUseCase {

    operator fun invoke(): List<String> {
        val slots = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        while (calendar.before(endCalendar)) {
            slots.add(timeFormat.format(calendar.time))
            calendar.add(Calendar.MINUTE, 30)
        }

        return slots
    }
}
