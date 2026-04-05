package com.refreshme.aistylefinder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class representing a hairstyle recommendation
 */
@Parcelize
data class AiStyleRecommendation(
    val styleName: String,
    val reasoning: String,
    val specialty: String,  // Used to match with stylist specialties
    val imageUrl: String? = null
) : Parcelable
