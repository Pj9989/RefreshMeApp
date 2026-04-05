package com.refreshme.aistylefinder

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Request model for AI style quiz
 */
@Parcelize
data class AiStyleRequest(
    val gender: String,      // women, men
    val vibe: String,        // clean_classic, bold_trendy, low_maintenance
    val frequency: String,   // weekly, biweekly, monthly
    val finish: String,      // natural, sharp, new
    val faceShape: String = "UNKNOWN" // OVAL, ROUND, SQUARE, HEART, UNKNOWN
) : Parcelable
