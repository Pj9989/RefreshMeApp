package com.refreshme.data

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val otherUserName: String = "",
    val otherUserProfileImageUrl: String = ""
)