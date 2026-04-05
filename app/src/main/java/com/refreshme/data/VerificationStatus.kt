package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
enum class VerificationStatus : Parcelable {
    @PropertyName("UNVERIFIED")
    UNVERIFIED,
    
    @PropertyName("unverified")
    unverified,
    
    @PropertyName("PENDING")
    PENDING,
    
    @PropertyName("pending")
    pending,
    
    @PropertyName("VERIFIED")
    VERIFIED,
    
    @PropertyName("verified")
    verified,
    
    @PropertyName("FAILED")
    FAILED,
    
    @PropertyName("failed")
    failed,
    
    @PropertyName("EXPIRED")
    EXPIRED,
    
    @PropertyName("expired")
    expired,
    
    @PropertyName("REQUIRES_INPUT")
    REQUIRES_INPUT,
    
    @PropertyName("requires_input")
    requires_input
}