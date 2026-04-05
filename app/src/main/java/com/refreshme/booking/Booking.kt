package com.refreshme.booking

import com.google.firebase.firestore.DocumentId
import com.refreshme.data.Service
import java.util.Date

data class Booking(
    @DocumentId
    var id: String = "", // Added for Firestore ID mapping and local use
    val stylistId: String = "",
    val stylistName: String = "",
    val userId: String = "",
    val userName: String = "",
    val service: Service = Service(),
    val dateTime: Date = Date(),
    val status: String = "Pending", // e.g., Pending, Confirmed, Cancelled
    val notes: String? = null
)