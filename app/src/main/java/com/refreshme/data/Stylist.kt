package com.refreshme.data

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.util.Date
import com.refreshme.data.GeoPointParceler 
import com.refreshme.data.DateParceler 

@IgnoreExtraProperties
@Parcelize
data class FlashDeal(
    val title: String = "",
    val description: String = "",
    val discountPercentage: Int = 0,
    val expiryTime: Date? = null,
    val serviceId: String? = null
) : Parcelable

@IgnoreExtraProperties
@Parcelize
data class SocialMediaLinks(
    val instagram: String? = null,
    val tiktok: String? = null,
    val website: String? = null
) : Parcelable

@IgnoreExtraProperties
@Parcelize
@TypeParceler<GeoPoint?, GeoPointParceler>
@TypeParceler<Date?, DateParceler>
data class Stylist(
    val id: String = "",
    val name: String = "",
    
    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl") var profileImageUrl: String? = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String? = "", 
    
    val rating: Double = 0.0,
    @get:Exclude @set:Exclude var distance: Double = 0.0,
    
    @get:PropertyName("availableNow") @set:PropertyName("availableNow") var isAvailable: Boolean? = false,
    @get:PropertyName("available") @set:PropertyName("available") var availableInDb: Boolean? = false, 
    
    val location: GeoPoint? = null,
    
    @get:PropertyName("verified") @set:PropertyName("verified") var isVerified: Boolean? = false,
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var verifiedInDb: Boolean? = false, 
    
    val specialty: String? = "",
    val vibes: List<String>? = emptyList(), // Feature: Hyper-Local Vibe Search
    val recommendedFaceShapes: List<String>? = emptyList(), // Feature: AI Consultations
    val services: List<Service>? = emptyList(),
    val bio: String? = "",
    val portfolioImages: List<String>? = emptyList(),
    val portfolioVideos: List<String>? = emptyList(), // Feature: Video Portfolio Reels
    val beforeAfterImages: List<BeforeAfter>? = emptyList(),
    val reviews: List<Review>? = emptyList(), 
    val address: String? = "", 
    
    // Safety & Monetization (Mobile / 24-7)
    @get:PropertyName("atHomeServiceFee") @set:PropertyName("atHomeServiceFee") var atHomeServiceFee: Double? = 0.0,
    @get:PropertyName("emergencyFee") @set:PropertyName("emergencyFee") var emergencyFee: Double? = 0.0,
    @get:PropertyName("travelBufferMinutes") @set:PropertyName("travelBufferMinutes") var travelBufferMinutes: Int? = 30,
    @get:PropertyName("requiresIdVerificationForMobile") @set:PropertyName("requiresIdVerificationForMobile") var requiresIdVerificationForMobile: Boolean? = true,

    @get:PropertyName("offersAtHomeService") @set:PropertyName("offersAtHomeService") var offersAtHomeService: Boolean? = false,
    @get:PropertyName("maxTravelRangeKm") @set:PropertyName("maxTravelRangeKm") var maxTravelRangeKm: Int? = 15,
    @get:PropertyName("online") @set:PropertyName("online") var isOnline: Boolean? = false, 
    @get:PropertyName("offersEventBooking") @set:PropertyName("offersEventBooking") var offersEventBooking: Boolean? = false,

    val serviceType: ServiceType = ServiceType.IN_SALON,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val workingHours: List<WorkingHours>? = emptyList(),
    val bookedAppointments: List<Appointment>? = emptyList(),
    val reviewCount: Int? = 0,
    @get:PropertyName("featured") @set:PropertyName("featured") var isFeatured: Boolean? = false,
    val galleryImageUrls: List<String>? = emptyList(),
    @get:PropertyName("yearsOfExperience") @set:PropertyName("yearsOfExperience") var yearsOfExperience: Int? = 0,
    val tools: List<String>? = emptyList(),
    val stripeAccountId: String? = null,
    val stripeAccountStatus: String? = null,
    val subscriptionTier: String = "BASIC",
    @get:PropertyName("subscriptionActive") @set:PropertyName("subscriptionActive") var isSubscriptionActive: Boolean = false,
    @get:PropertyName("trialStartTime") @set:PropertyName("trialStartTime") var trialStartTime: Date? = null,
    @get:PropertyName("lastOnlineAt") @set:PropertyName("lastOnlineAt") var lastOnlineAt: Date? = null,
    val socialLinks: SocialMediaLinks? = null, // Feature: Profile Discovery & Enhancements
    @get:Exclude @set:Exclude var matchScore: Int = 0, 
    @get:Exclude @set:Exclude var matchExplanation: String? = null,
    val currentFlashDeal: FlashDeal? = null // Feature: Dynamic Flash Deals
) : Parcelable {
    
    @get:Exclude
    val displayImageUrl: String?
        get() = profileImageUrl?.ifBlank { imageUrl } ?: imageUrl
    
    @get:Exclude
    val isVerifiedStylist: Boolean
        get() = isVerified == true || verifiedInDb == true
        
    @get:Exclude
    val isCurrentlyAvailable: Boolean
        get() = isAvailable == true || availableInDb == true

    @get:Exclude
    val hasActiveFlashDeal: Boolean
        get() = currentFlashDeal != null && (currentFlashDeal.expiryTime == null || currentFlashDeal.expiryTime.after(Date()))
}