package com.refreshme.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TimeSlot(
    val time: String = "",
    val isBooked: Boolean = false
) : Parcelable
