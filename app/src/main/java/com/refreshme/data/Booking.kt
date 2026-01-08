package com.refreshme.data

import java.util.Date

data class Booking(
    val stylistId: String = "",
    val userId: String = "",
    val date: Date = Date(),
    val status: String = ""
)