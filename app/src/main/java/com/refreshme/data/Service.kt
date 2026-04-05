package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Service(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    @get:PropertyName("originalPrice") @set:PropertyName("originalPrice") var originalPrice: Double? = null,
    @PropertyName("durationMinutes") var durationMinutes: Int? = 0 // in minutes
) : Parcelable {
    @get:PropertyName("isOnSale")
    val isOnSale: Boolean
        get() = originalPrice != null && originalPrice!! > price
}
