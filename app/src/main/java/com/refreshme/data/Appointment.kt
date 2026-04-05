package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Appointment(
    @get:PropertyName("dateTime") @set:PropertyName("dateTime") var startTimeMillis: Long = 0,
    @get:PropertyName("duration") @set:PropertyName("duration") var durationMinutes: Int = 0,
    @get:PropertyName("isSilentAppointment") @set:PropertyName("isSilentAppointment") var isSilentAppointment: Boolean = false,
    @get:PropertyName("isSensoryFriendly") @set:PropertyName("isSensoryFriendly") var isSensoryFriendly: Boolean = false
) : Parcelable
