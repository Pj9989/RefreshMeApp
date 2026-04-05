package com.refreshme.stylist

import com.google.firebase.Timestamp
import com.refreshme.data.BookingStatus

// Data class used in StylistBookingsFragment
data class StylistBooking(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerPhoto: String? = null,
    val serviceName: String,
    val price: Double,
    val startTime: Timestamp? = null,
    val location: String,
    val status: BookingStatus,
    val notes: String,
    val createdAt: Timestamp? = null,
    val isSilentAppointment: Boolean = false,
    val isSensoryFriendly: Boolean = false
) {
    /**
     * Format booking time for display
     */
    fun getFormattedTime(): String {
        val date = startTime?.toDate() ?: return "Time not set" 
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Format price for display
     */
    fun getFormattedPrice(): String {
        return "$${"%.2f".format(price)}"
    }
}
