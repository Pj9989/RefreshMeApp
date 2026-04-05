package com.refreshme.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Date

@IgnoreExtraProperties
@Keep
@Parcelize
data class Review(
    val userId: String = "",
    val userName: String = "Verified User",
    val stylistId: String = "", // Used for both stylist being rated or customer being rated
    val rating: Double = 0.0,
    val comment: String = "",
    // Exclude from direct Firestore mapping to use the robust setter below
    @get:Exclude @set:Exclude var timestampMillis: Long? = null,
    val imageUrls: List<String> = emptyList()
) : Parcelable {
    
    // This property handles both Long and Timestamp types from Firestore
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var firestoreTimestamp: @RawValue Any?
        get() = timestampMillis
        set(value) {
            timestampMillis = when (value) {
                is Timestamp -> value.toDate().time
                is Long -> value
                else -> null
            }
        }

    @get:Exclude
    val timestampAsDate: Date?
        get() = timestampMillis?.let { Date(it) }
}
