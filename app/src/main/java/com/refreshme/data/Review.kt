package com.refreshme.data

data class Review(
    val userId: String = "",
    val stylistId: String = "",
    val rating: Float = 0f,
    val text: String = "",
    val timestamp: Long = 0
)
