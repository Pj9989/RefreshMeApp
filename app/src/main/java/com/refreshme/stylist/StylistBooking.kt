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
    val price: Double = 0.0, // Renamed from servicePrice
    val startTime: Timestamp? = null, // Renamed from bookingTime
    val location: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val notes: String = "",
    val createdAt: Timestamp? = null
) {
    /**
     * Format booking time for display
     */
    fun getFormattedTime(): String {
        val date = startTime?.toDate() ?: return "Time not set" // Use startTime
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Format price for display
     */
    fun getFormattedPrice(): String {
        return "$${"%.2f".format(price)}" // Use price
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
