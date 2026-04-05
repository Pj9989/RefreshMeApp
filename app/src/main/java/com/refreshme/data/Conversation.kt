package com.refreshme.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "", // Added for unread tracking
    @ServerTimestamp val lastMessageTime: Date? = null, // Added for sorting and display
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserProfileImageUrl: String = ""
)
