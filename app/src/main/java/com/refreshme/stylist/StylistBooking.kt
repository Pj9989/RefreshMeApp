package com.refreshme.stylist

import com.google.firebase.Timestamp

/**
 * Data model for stylist bookings
 */
data class StylistBooking(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerPhoto: String? = null,
    val serviceName: String = "",
    val servicePrice: Double = 0.0,
    val bookingTime: Timestamp? = null,
    val location: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val notes: String = "",
    val createdAt: Timestamp? = null
) {
    /**
     * Format booking time for display
     */
    fun getFormattedTime(): String {
        val date = bookingTime?.toDate() ?: return "Time not set"
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Format price for display
     */
    fun getFormattedPrice(): String {
        return "$${"%.2f".format(servicePrice)}"
    }
}

/**
 * Booking status enum
 */
enum class BookingStatus {
    PENDING,      // Waiting for stylist confirmation
    CONFIRMED,    // Stylist confirmed
    IN_PROGRESS,  // Service started
    COMPLETED,    // Service completed
    CANCELLED     // Booking cancelled
}
