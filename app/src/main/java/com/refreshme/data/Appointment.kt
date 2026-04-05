package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Appointment(
    @get:PropertyName("dateTime") @set:PropertyName("dateTime") var startTimeMillis: Long = 0,
    @get:PropertyName("duration") @set:PropertyName("duration") var durationMinutes: Int = 0
) : Parcelable
