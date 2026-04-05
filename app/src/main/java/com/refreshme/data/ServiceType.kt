package com.refreshme.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ServiceType : Parcelable {
    AT_HOME,
    IN_SALON,
    ALL_HOURS
}