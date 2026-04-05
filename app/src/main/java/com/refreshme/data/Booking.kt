package com.refreshme.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
enum class BookingStatus {
    REQUESTED,
    PENDING,
    ACCEPTED,
    DEPOSIT_PAID,
    DECLINED,
    ON_THE_WAY, // Newly added for house calls
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REFUND_PROCESSING
}

@IgnoreExtraProperties
@Keep
data class Booking(
    var id: String = "",
    val stylistId: String = "",
    val stylistName: String = "",
    val stylistPhotoUrl: String? = null,
    val customerId: String = "",
    val customerName: String = "",
    val customerPhotoUrl: String? = null,
    val serviceName: String = "",
    val notes: String = "",
    val priceCents: Long = 0,
    val status: String = BookingStatus.REQUESTED.name,
    val requestedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val scheduledStart: Timestamp? = null,
    val isRated: Boolean = false,
    val isCustomerRated: Boolean = false,
    
    // Safety & Monetization fields
    val emergencyFeeApplied: Double? = 0.0,
    val travelFeeApplied: Double? = 0.0,
    val isIdVerified: Boolean = false,
    
    // Mobile Booking / Uber for Barbers fields
    val isMobile: Boolean = false,
    val customerAddress: String? = null,
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val stylistLat: Double? = null,
    val stylistLng: Double? = null,

    // Group & Special Event Booking
    val isEvent: Boolean = false,
    val groupSize: Int = 1,
    val eventType: String? = null, // e.g., "Wedding", "Birthday", "Corporate"
    val additionalParticipants: List<String> = emptyList(),

    // The Modern Salon Experience
    val isSilentAppointment: Boolean = false,
    val isSensoryFriendly: Boolean = false
) {
    // Helper to get the enum safely
    val bookingStatus: BookingStatus
        get() = try {
            BookingStatus.valueOf(status)
        } catch (e: Exception) {
            BookingStatus.REQUESTED
        }
}