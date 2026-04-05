package com.refreshme.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// FIX: Creating a Customer data class with default values to generate the no-argument constructor for Firebase deserialization
@Parcelize
data class Customer(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val phone: String? = null,
    val address: String? = null
) : Parcelable