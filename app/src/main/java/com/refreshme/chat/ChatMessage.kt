package com.refreshme.chat

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
