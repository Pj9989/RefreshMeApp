package com.refreshme

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
enum class Role {
    CUSTOMER,
    STYLIST
}

@Keep
data class StyleProfile(
    val gender: String = "",
    val vibe: String = "",
    val frequency: String = "",
    val finish: String = "",
    val hairType: String = "",
    val faceShape: String = "",
    val lastUpdated: Long = 0
)

@Keep
@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    
    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl") var profileImageUrl: String? = null,
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String? = null, 
    
    @get:PropertyName("role") 
    @set:PropertyName("role") 
    var userRoleValue: String = "CUSTOMER",
    
    val styleProfile: StyleProfile? = null,
    val createdAt: Timestamp? = null,
    val subscriptionStatus: String? = null,
    val verified: Boolean? = false,
    val subscriptionId: String? = null,
    val stripeCustomerId: String? = null
) {
    @get:Exclude
    val role: Role
        get() = when (userRoleValue.uppercase()) {
            "STYLIST" -> Role.STYLIST
            else -> Role.CUSTOMER
        }
        
    @get:Exclude
    val displayImageUrl: String?
        get() = if (!profileImageUrl.isNullOrBlank()) profileImageUrl else if (!imageUrl.isNullOrBlank()) imageUrl else null
}