package com.refreshme.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
// import com.google.maps.android.clustering.ClusterItem
import java.util.Calendar

enum class ServiceType {
    AT_HOME,
    IN_SALON,
    ALL_HOURS
}

enum class SubscriptionTier {
    BASIC,
    PRO
}

data class WorkingHours(
    val dayOfWeek: Int = 0, // Sunday is 1, Saturday is 7
    val startTime: String = "09:00", // 24-hour format
    val endTime: String = "17:00"
)

data class Appointment(
    val dateTime: Long = 0L, // Unix timestamp
    val duration: Int = 60 // in minutes
)

@IgnoreExtraProperties
data class Stylist(
    val id: String = "",
    val name: String = "",
    val location: GeoPoint? = null,
    val profileImageUrl: String? = null,
    val rating: Double = 0.0,
    val services: List<Service>? = null,
    val atHomeServiceFee: Double? = null,
    val yearsOfExperience: Int? = null,
    val reviewCount: Int? = null,
    val address: String? = null,
    val bio: String? = null,
    val specialty: String? = null,
    val offersAtHomeService: Boolean? = null,
    val portfolioImages: Map<String, String>? = null,
    @get:PropertyName("verified") @set:PropertyName("verified")
    var isVerified: Boolean = false,
    val workingHours: List<WorkingHours>? = null,
    val bookedAppointments: List<Appointment>? = null,
    @get:PropertyName("online") @set:PropertyName("online")
    var isOnline: Boolean = false,
    val galleryImageUrls: List<String>? = null,
    val reviews: List<Review>? = null,
    val isFeatured: Boolean = false,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.BASIC
) /*: ClusterItem */ {
    /*
    @get:Exclude
    override fun getPosition(): LatLng {
        return LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    @get:Exclude
    override fun getTitle(): String {
        return name
    }

    @get:Exclude
    override fun getSnippet(): String {
        return specialty ?: ""
    }

    @get:Exclude
    override fun getZIndex(): Float {
        return 0f
    }
    */

    @get:Exclude
    val isAvailableNow: Boolean
        get() = isOnline

    @get:Exclude
    val serviceType: ServiceType
        get() {
            val is247 = workingHours?.any { it.startTime == "00:00" && it.endTime == "24:00" } == true
            return when {
                is247 -> ServiceType.ALL_HOURS
                offersAtHomeService == true -> ServiceType.AT_HOME
                else -> ServiceType.IN_SALON
            }
        }
}
