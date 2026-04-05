package com.refreshme.chat

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
enum class MessageType {
    TEXT,
    STYLE_PROFILE,
    IMAGE
}

@Keep
data class ChatMessage(
    val id: String = "", // Added ID to track specific messages
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp val timestamp: Date? = null,
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String>? = null,
    val read: Boolean = false // Added read status
)
