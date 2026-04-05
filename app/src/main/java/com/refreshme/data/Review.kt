package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Date

@Parcelize
data class Review(
    val userId: String = "",
    val userName: String = "Verified Client",
    val stylistId: String = "",
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
