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

const val DEFAULT_AT_HOME_SERVICE_FEE = 20.0

@IgnoreExtraProperties
@Parcelize
data class FlashDeal(
    var title: String = "",
    var description: String = "",
    var discountPercentage: Int = 0,
    var expiryTime: Date? = null,
    var serviceId: String? = null
) : Parcelable

@IgnoreExtraProperties
@Parcelize
data class SocialMediaLinks(
    var instagram: String? = null,
    var tiktok: String? = null,
    var website: String? = null
) : Parcelable

@IgnoreExtraProperties
@Parcelize
@TypeParceler<GeoPoint?, GeoPointParceler>
@TypeParceler<Date?, DateParceler>
data class Stylist(
    var id: String = "",
    var name: String = "",

    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl") var profileImageUrl: String? = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String? = "",

    var rating: Double = 0.0,
    @get:Exclude @set:Exclude var distance: Double = 0.0,
    
    @get:PropertyName("availableNow") @set:PropertyName("availableNow") var isAvailable: Boolean? = false,
    @get:PropertyName("available") @set:PropertyName("available") var availableInDb: Boolean? = false, 
    
    @get:PropertyName("location") @set:PropertyName("location") var location: GeoPoint? = null,
    
    @get:PropertyName("verified") @set:PropertyName("verified") var isVerified: Boolean? = false,
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var verifiedInDb: Boolean? = false, 
    
    var specialty: String? = "",
    var vibes: List<String>? = emptyList(), // Feature: Hyper-Local Vibe Search
    var recommendedFaceShapes: List<String>? = emptyList(), // Feature: AI Consultations
    var services: List<Service>? = emptyList(),
    var bio: String? = "",
    var portfolioImages: List<String>? = emptyList(),
    var portfolioVideos: List<String>? = emptyList(), // Feature: Video Portfolio Reels
    var beforeAfterImages: List<BeforeAfter>? = emptyList(),
    var reviews: List<Review>? = emptyList(),
    var address: String? = "",
    @get:PropertyName("salonAddress") @set:PropertyName("salonAddress") var salonAddress: String? = "",
    @get:PropertyName("serviceLocationType") @set:PropertyName("serviceLocationType") var serviceLocationType: String? = null,
    
    // Safety & Monetization (Mobile / 24-7)
    @get:PropertyName("atHomeServiceFee") @set:PropertyName("atHomeServiceFee") var atHomeServiceFee: Double? = 0.0,
    @get:PropertyName("emergencyFee") @set:PropertyName("emergencyFee") var emergencyFee: Double? = 0.0,
    @get:PropertyName("travelBufferMinutes") @set:PropertyName("travelBufferMinutes") var travelBufferMinutes: Int? = 30,
    @get:PropertyName("requiresIdVerificationForMobile") @set:PropertyName("requiresIdVerificationForMobile") var requiresIdVerificationForMobile: Boolean? = true,

    @get:PropertyName("offersAtHomeService") @set:PropertyName("offersAtHomeService") var offersAtHomeService: Boolean? = false,
    @get:PropertyName("maxTravelRangeKm") @set:PropertyName("maxTravelRangeKm") var maxTravelRangeKm: Int? = 15,
    @get:PropertyName("online") @set:PropertyName("online") var isOnline: Boolean? = false, 
    @get:PropertyName("offersEventBooking") @set:PropertyName("offersEventBooking") var offersEventBooking: Boolean? = false,

    var serviceType: ServiceType = ServiceType.IN_SALON,
    var verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    var workingHours: List<WorkingHours>? = emptyList(),
    var bookedAppointments: List<Appointment>? = emptyList(),
    var reviewCount: Int? = 0,
    @get:PropertyName("featured") @set:PropertyName("featured") var isFeatured: Boolean? = false,
    var galleryImageUrls: List<String>? = emptyList(),
    @get:PropertyName("yearsOfExperience") @set:PropertyName("yearsOfExperience") var yearsOfExperience: Int? = 0,
    var tools: List<String>? = emptyList(),
    var stripeAccountId: String? = null,
    var stripeAccountStatus: String? = null,
    var subscriptionTier: String = "BASIC",
    @get:PropertyName("subscriptionActive") @set:PropertyName("subscriptionActive") var isSubscriptionActive: Boolean = false,
    @get:PropertyName("trialStartTime") @set:PropertyName("trialStartTime") var trialStartTime: Date? = null,
    @get:PropertyName("lastOnlineAt") @set:PropertyName("lastOnlineAt") var lastOnlineAt: Date? = null,
    var socialLinks: SocialMediaLinks? = null, // Feature: Profile Discovery & Enhancements
    var shopId: String? = null,
    var businessName: String? = null,
    var businessBio: String? = null,
    var businessAddress: String? = null,
    var businessPhone: String? = null,
    var businessWebsite: String? = null,
    var isShopProfile: Boolean = false,
    @get:Exclude @set:Exclude var matchScore: Int = 0, 
    @get:Exclude @set:Exclude var matchExplanation: String? = null,
    var currentFlashDeal: FlashDeal? = null, // Feature: Dynamic Flash Deals
    @get:PropertyName("servesGender") @set:PropertyName("servesGender")
    var servesGender: List<String>? = listOf("Men", "Women", "Non-binary"),

    /**
     * Professional categories this pro offers. Canonical ids come from
     * [StylistCategories] (hair / makeup / nails). Defaults to ["hair"] for
     * any pre-existing doc that was written before this field existed so
     * older stylists keep appearing on the Home list.
     */
    var categories: List<String>? = listOf(StylistCategories.HAIR)
) : Parcelable {
    
    @get:Exclude
    val displayImageUrl: String?
        get() = profileImageUrl?.ifBlank { imageUrl } ?: imageUrl

    @get:Exclude
    val hasPublicPhoto: Boolean
        get() = !displayImageUrl.isNullOrBlank() ||
            !portfolioImages.isNullOrEmpty() ||
            !portfolioVideos.isNullOrEmpty() ||
            !galleryImageUrls.isNullOrEmpty() ||
            beforeAfterImages.orEmpty().any {
                !it.beforeImageUrl.isNullOrBlank() || !it.afterImageUrl.isNullOrBlank()
            }

    @get:Exclude
    val hasPublicProfileSetup: Boolean
        get() = name.isNotBlank() &&
            (
                !specialty.isNullOrBlank() ||
                !bio.isNullOrBlank() ||
                !services.isNullOrEmpty() ||
                !displayAddress.isNullOrBlank() ||
                !businessName.isNullOrBlank() ||
                !businessBio.isNullOrBlank()
            )

    @get:Exclude
    val isDiscoverable: Boolean
        get() = isVerifiedStylist && hasPublicPhoto && hasPublicProfileSetup
    
    @get:Exclude
    val isVerifiedStylist: Boolean
        get() = isVerified == true || verifiedInDb == true
        
    @get:Exclude
    val isCurrentlyAvailable: Boolean
        get() = isAvailable == true || availableInDb == true

    @get:Exclude
    val offersMobileService: Boolean
        get() = offersAtHomeService == true ||
            serviceType == ServiceType.AT_HOME ||
            serviceType == ServiceType.ALL_HOURS ||
            serviceType == ServiceType.AFTER_HOURS

    @get:Exclude
    val displayAddress: String?
        get() = listOf(salonAddress, address, businessAddress)
            .firstOrNull { !it.isNullOrBlank() }

    @get:Exclude
    val hasFixedPublicLocation: Boolean
        get() {
            val loc = location ?: return false
            if (loc.latitude == 0.0 && loc.longitude == 0.0) return false
            val fixedByType = serviceLocationType == "fixed"
            val legacyFixedProfile = serviceLocationType.isNullOrBlank() && !offersMobileService
            return (fixedByType || legacyFixedProfile) && !displayAddress.isNullOrBlank()
        }

    @get:Exclude
    val effectiveAtHomeServiceFee: Double
        get() {
            val savedFee = atHomeServiceFee ?: 0.0
            return if (savedFee > 0.0) savedFee else if (offersMobileService) DEFAULT_AT_HOME_SERVICE_FEE else 0.0
        }

    @get:Exclude
    val hasActiveFlashDeal: Boolean
        get() {
            val deal = currentFlashDeal ?: return false
            val expiry = deal.expiryTime
            return expiry == null || expiry.after(Date())
        }
}
