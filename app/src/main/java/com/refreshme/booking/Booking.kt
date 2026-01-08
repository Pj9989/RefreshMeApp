package com.refreshme.booking

import com.refreshme.data.Service
import java.util.Date

data class Booking(
    val stylistId: String = "",
    val stylistName: String = "",
    val userId: String = "",
    val userName: String = "",
    val service: Service = Service(),
    val dateTime: Date = Date(),
    val status: String = "Pending", // e.g., Pending, Confirmed, Cancelled
    val notes: String? = null
)