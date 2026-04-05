package com.refreshme.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WorkingHours(
    val dayOfWeek: Int = 0,
    val startTime: String = "",
    val endTime: String = ""
) : Parcelable