package com.refreshme.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import java.util.Date

@Keep
data class WaitlistEntry(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val stylistId: String = "",
    val targetDate: Long = 0L,
    val createdAt: Timestamp? = null
) {
    fun getFormattedTargetDate(): String {
        if (targetDate == 0L) return "Unknown Date"
        val format = java.text.SimpleDateFormat("EEEE, MMM d, yyyy", java.util.Locale.getDefault())
        return format.format(Date(targetDate))
    }
}