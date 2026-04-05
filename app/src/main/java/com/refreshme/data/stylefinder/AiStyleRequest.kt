package com.refreshme.data.stylefinder

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AiStyleRequest(
    val uid: String = "",
    val answers: Map<String, Any> = emptyMap(),
    val status: String = "queued",
    @ServerTimestamp
    val createdAt: Date? = null,
    val result: AiStyleResult? = null,
    val error: String? = null
)
