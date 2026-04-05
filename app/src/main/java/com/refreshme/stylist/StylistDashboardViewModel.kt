package com.refreshme.stylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.refreshme.data.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardStats(
    val totalEarnings: Double = 0.0,
    val pendingRequests: Int = 0,
    val upcomingBookings: Int = 0,
    val completedThisMonth: Int = 0,
    val weeklyEarnings: List<Double> = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
)

data class FlashDeal(
    val title: String = "",
    val discountPercentage: Int = 0,
    val expiresAtMillis: Long = 0
)

class StylistDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bookingsListener: ListenerRegistration? = null
    private var stylistListener: ListenerRegistration? = null
    private var reviewsListener: ListenerRegistration? = null

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline
    
    private val _isMobile = MutableStateFlow(false)
    val isMobile: StateFlow<Boolean> = _isMobile

    private val _offersEvents = MutableStateFlow(false)
    val offersEvents: StateFlow<Boolean> = _offersEvents.asStateFlow()

    private val _stylistName = MutableStateFlow("Stylist")
    val stylistName: StateFlow<String> = _stylistName

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl
    
    private val _rating = MutableStateFlow(0.0)
    val rating: StateFlow<Double> = _rating
    
    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount

    private val _currentFlashDeal = MutableStateFlow<FlashDeal?>(null)
    val currentFlashDeal: StateFlow<FlashDeal?> = _currentFlashDeal

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                listenToStylistData(user.uid)
                listenToBookings(user.uid)
                listenToReviews(user.uid)
            } else {
                bookingsListener?.remove()
                stylistListener?.remove()
                reviewsListener?.remove()
                bookingsListener = null
                stylistListener = null
                reviewsListener = null
            }
        }
    }

    private fun listenToStylistData(uid: String) {
        stylistListener?.remove()
        stylistListener = firestore.collection("stylists").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StylistDashboardViewModel", "Error listening to stylist data", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val map = snapshot.data ?: return@addSnapshotListener
                    
                    _stylistName.value = map["name"] as? String ?: "Stylist"
                    
                    val profileImageUrl = map["profileImageUrl"] as? String
                    val imageUrl = map["imageUrl"] as? String
                    val displayImageUrl = profileImageUrl?.ifBlank { imageUrl } ?: imageUrl
                    _profileImageUrl.value = displayImageUrl
                    
                    val online = map["online"] as? Boolean ?: map["availableNow"] as? Boolean ?: map["available"] as? Boolean ?: false
                    _isOnline.value = online
                    
                    val mobile = map["offersAtHomeService"] as? Boolean ?: false
                    _isMobile.value = mobile

                    val event = map["offersEventBooking"] as? Boolean ?: false
                    _offersEvents.value = event
                    
                    _rating.value = (map["rating"] as? Number)?.toDouble() ?: 0.0
                    _reviewCount.value = (map["reviewCount"] as? Number)?.toInt() ?: 0

                    val hasFlashDeal = map["hasActiveFlashDeal"] as? Boolean ?: false
                    if (hasFlashDeal) {
                        val dealMap = map["currentFlashDeal"] as? Map<String, Any>
                        if (dealMap != null) {
                            val title = dealMap["title"] as? String ?: ""
                            val discount = (dealMap["discountPercentage"] as? Number)?.toInt() ?: 0
                            val expires = (dealMap["expiresAtMillis"] as? Number)?.toLong() ?: 0L
                            _currentFlashDeal.value = FlashDeal(title, discount, expires)
                        } else {
                            _currentFlashDeal.value = null
                        }
                    } else {
                        _currentFlashDeal.value = null
                    }
                }
            }
    }

    private fun listenToBookings(uid: String) {
        bookingsListener?.remove()
        
        // Listen to all bookings for this stylist to calculate stats
        bookingsListener = firestore.collection("bookings")
            .whereEqualTo("stylistId", uid)
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StylistDashboardViewModel", "Error listening to bookings", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var earnings = 0.0
                    var pending = 0
                    var upcoming = 0
                    var completedMonth = 0
                    
                    val now = System.currentTimeMillis()
                    val cal = Calendar.getInstance()
                    val currentMonth = cal.get(Calendar.MONTH)
                    val currentYear = cal.get(Calendar.YEAR)
                    
                    // Simple logic for weekly distribution
                    val weeklyEarnings = mutableListOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

                    for (doc in snapshot.documents) {
                        val status = doc.getString("status") ?: ""
                        val price = (doc.getDouble("price")) 
                            ?: (doc.getLong("priceCents")?.toDouble()?.div(100))
                            ?: 0.0
                        val timestamp = doc.getLong("timestampMillis") ?: 0L
                        
                        when (status) {
                            "PENDING", "REQUESTED" -> pending++
                            "ACCEPTED", "CONFIRMED" -> {
                                if (timestamp > now) upcoming++
                            }
                            "COMPLETED" -> {
                                earnings += price
                                
                                cal.timeInMillis = timestamp
                                if (cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear) {
                                    completedMonth++
                                }
                                
                                // A very rough weekly calculation (last 7 days)
                                val daysAgo = ((now - timestamp) / (1000 * 60 * 60 * 24)).toInt()
                                if (daysAgo in 0..6) {
                                    weeklyEarnings[6 - daysAgo] += price
                                }
                            }
                        }
                    }
                    
                    _stats.value = DashboardStats(
                        totalEarnings = earnings,
                        pendingRequests = pending,
                        upcomingBookings = upcoming,
                        completedThisMonth = completedMonth,
                        weeklyEarnings = weeklyEarnings
                    )
                }
            }
    }

    private fun listenToReviews(uid: String) {
        reviewsListener?.remove()
        reviewsListener = firestore.collection("stylists").document(uid).collection("reviews")
            .orderBy("timestampMillis", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StylistDashboardViewModel", "Error listening to reviews", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _reviews.value = snapshot.toObjects(Review::class.java)
                }
            }
    }

    fun toggleOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        _isOnline.value = online
        
        val updates = mapOf(
            "online" to online, 
            "availableNow" to online,
            "available" to online
        )
        
        firestore.collection("stylists").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("StylistDashboardViewModel", "Successfully updated online status to $online")
            }
            .addOnFailureListener { e ->
                Log.e("StylistDashboardViewModel", "Failed to update online status", e)
                // Revert state on failure
                _isOnline.value = !online
            }
    }
    
    fun toggleMobileStatus(isMobile: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        _isMobile.value = isMobile
        
        firestore.collection("stylists").document(uid)
            .update("offersAtHomeService", isMobile)
            .addOnFailureListener {
                _isMobile.value = !isMobile
            }
    }

    fun toggleEventStatus(offers: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        _offersEvents.value = offers

        firestore.collection("stylists").document(uid)
            .update("offersEventBooking", offers)
            .addOnFailureListener {
                _offersEvents.value = !offers
            }
    }

    fun requestPayout() {
        // Just mock a payout request and update totalEarnings slightly for demonstration.
        // In a real app this would trigger a Cloud Function connected to Stripe.
        val currentEarnings = _stats.value.totalEarnings
        if (currentEarnings > 0) {
            _stats.value = _stats.value.copy(totalEarnings = 0.0)
        }
    }

    fun createFlashDeal(title: String, discountPercentage: Int, durationHours: Int) {
        val uid = auth.currentUser?.uid ?: return
        val expiresAt = System.currentTimeMillis() + (durationHours * 60 * 60 * 1000L)
        val dealMap = mapOf(
            "title" to title,
            "discountPercentage" to discountPercentage,
            "expiresAtMillis" to expiresAt
        )

        firestore.collection("stylists").document(uid)
            .update(
                "hasActiveFlashDeal", true,
                "currentFlashDeal", dealMap
            )
    }

    fun clearFlashDeal() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(uid)
            .update(
                "hasActiveFlashDeal", false,
                "currentFlashDeal", null
            )
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
        stylistListener?.remove()
        reviewsListener?.remove()
    }
}