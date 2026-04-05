package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Service(
    var id: String = "",
    var name: String = "",
    var description: String = "", 
    var price: Double = 0.0,
    
    @get:PropertyName("originalPrice") @set:PropertyName("originalPrice") 
    var originalPrice: Double? = null,
    
    @get:PropertyName("durationMinutes") @set:PropertyName("durationMinutes") 
    var durationMinutes: Int? = 0,
    
    @get:PropertyName("isBundle") @set:PropertyName("isBundle")
    var isBundle: Boolean = false
) : Parcelable {
    
    @get:PropertyName("isOnSale")
    val isOnSale: Boolean
        get() = originalPrice != null && originalPrice!! > price

    // Firestore fallback for when the field is named "bundle" instead of "isBundle"
    @PropertyName("bundle")
    fun setBundleLegacy(value: Boolean) {
        this.isBundle = value
    }

    @PropertyName("bundle")
    fun getBundleLegacy(): Boolean = isBundle
}
