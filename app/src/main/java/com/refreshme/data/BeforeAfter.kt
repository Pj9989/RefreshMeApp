package com.refreshme.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.util.Date

@Parcelize
@TypeParceler<Date?, DateParceler>
data class BeforeAfter(
    val id: String = "",
    val beforeImageUrl: String = "",
    val afterImageUrl: String = "",
    val description: String = "",
    val technicalNotes: String = "", 
    val tags: List<String> = emptyList(),
    val timestamp: Date? = null
) : Parcelable
