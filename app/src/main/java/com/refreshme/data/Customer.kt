package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

// FIX: Creating a Customer data class with default values to generate the no-argument constructor for Firebase deserialization
@IgnoreExtraProperties
@Parcelize
data class Customer(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val phone: String? = null,
    val address: String? = null
) : Parcelable