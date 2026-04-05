package com.refreshme.app.data.model

import com.google.firebase.firestore.PropertyName

data class AiStyleRecommendation(
    @get:PropertyName("hairstyleName") val hairstyleName: String = "",
    @get:PropertyName("description") val description: String = "",
    @get:PropertyName("matchingStylists") val matchingStylists: List<String> = emptyList(), // List of Stylist UIDs
    @get:PropertyName("imageUrl") val imageUrl: String = ""
)
