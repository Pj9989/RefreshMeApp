package com.refreshme.stylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.refreshme.data.FlashDeal
import com.refreshme.data.Review
import com.refreshme.data.Stylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class DashboardStats(
    val totalEarnings: Double = 0.0,
    val pendingRequests: Int = 0,
    val upcomingBookings: Int = 0,
    val completedThisMonth: Int = 0
)

class StylistDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bookingsListener: ListenerRegistration? = null
    private var stylistListener: ListenerRegistration? = null

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline
    
    private val _isMobile = MutableStateFlow(false)
    val isMobile: StateFlow<Boolean> = _isMobile

    private val _stylistName = MutableStateFlow("Stylist")
    val stylistName: StateFlow<String> = _stylistName

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl

    private val _rating = MutableStateFlow(0.0)
    val rating: StateFlow<Double> = _rating

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _currentFlashDeal = MutableStateFlow<FlashDeal?>(null)
    val currentFlashDeal: StateFlow<FlashDeal?> = _currentFlashDeal

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val uid = auth.currentUser?.uid ?: return
        
        bookingsListener?.remove()
        stylistListener?.remove()

        stylistListener = firestore.collection("stylists").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e("StylistDashboardViewModel", "Error fetching stylist data", error)
                    return@addSnapshotListener
                }
                
                doc?.let {
                    if (it.exists()) {
                        val stylistData = it.toObject(Stylist::class.java)
                        _isOnline.value = it.getBoolean("online") ?: it.getBoolean("availableNow") ?: it.getBoolean("available") ?: false
                        _isMobile.value = it.getBoolean("offersAtHomeService") ?: false
                        _stylistName.value = it.getString("name") ?: "Stylist"
                        _profileImageUrl.value = stylistData?.displayImageUrl
                        _rating.value = it.getDouble("rating") ?: 0.0
                        
                        val reviewsList = it.get("reviews") as? List<Map<String, Any>>
                        _reviews.value = reviewsList?.map { map ->
                            Review(
                                userId = map["userId"] as? String ?: "",
                                userName = map["userName"] as? String ?: "Client",
                                stylistId = map["stylistId"] as? String ?: "",
                                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                                comment = map["comment"] as? String ?: "",
                                timestampMillis = when (val ts = map["timestamp"]) {
                                    is com.google.firebase.Timestamp -> ts.toDate().time
                                    is Long -> ts
                                    else -> null
                                }
                            )
                        } ?: emptyList()
                        
                        // Load current flash deal if exists
                        val dealMap = it.get("currentFlashDeal") as? Map<String, Any>
                        if (dealMap != null) {
                            _currentFlashDeal.value = FlashDeal(
                                title = dealMap["title"] as? String ?: "",
                                discountPercentage = (dealMap["discountPercentage"] as? Long)?.toInt() ?: 0,
                                expiryTime = (dealMap["expiryTime"] as? com.google.firebase.Timestamp)?.toDate()
                            )
                        } else {
                            _currentFlashDeal.value = null
                        }
                    }
                }
            }

        bookingsListener = firestore.collection("bookings")
            .whereEqualTo("stylistId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StylistDashboardViewModel", "Error fetching bookings", error)
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    var earnings = 0.0
                    var pending = 0
                    var upcoming = 0
                    var completedMonth = 0
                    
                    val calendar = Calendar.getInstance()
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    it.documents.forEach { doc ->
                        val status = doc.getString("status") ?: ""
                        
                        val price = doc.getDouble("servicePrice") 
                            ?: doc.getDouble("price") 
                            ?: doc.getDouble("fullPrice") 
                            ?: (doc.getLong("priceCents")?.toDouble()?.div(100))
                            ?: 0.0
                            
                        val startTime = doc.getTimestamp("date")?.toDate() 
                            ?: doc.getTimestamp("startTime")?.toDate()
                            ?: doc.getTimestamp("bookingDate")?.toDate()
                            ?: doc.getTimestamp("requestedStartTime")?.toDate()

                        when (status.uppercase()) {
                            "PENDING", "REQUESTED", "PENDING_PAYMENT" -> pending++
                            "ACCEPTED", "IN_PROGRESS", "CONFIRMED", "DEPOSIT_PAID" -> upcoming++
                            "COMPLETED", "PAID_IN_FULL" -> {
                                earnings += price
                                startTime?.let { date ->
                                    calendar.time = date
                                    if (calendar.get(Calendar.MONTH) == currentMonth && 
                                        calendar.get(Calendar.YEAR) == currentYear) {
                                        completedMonth++
                                    }
                                }
                            }
                        }
                    }
                    
                    _stats.value = DashboardStats(
                        totalEarnings = earnings,
                        pendingRequests = pending,
                        upcomingBookings = upcoming,
                        completedThisMonth = completedMonth
                    )
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
        
        val updates = mapOf(
            "offersAtHomeService" to isMobile
        )
        
        firestore.collection("stylists").document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("StylistDashboardViewModel", "Successfully updated mobile status to $isMobile")
            }
            .addOnFailureListener { e ->
                Log.e("StylistDashboardViewModel", "Failed to update mobile status", e)
                // Revert state on failure
                _isMobile.value = !isMobile
            }
    }

    fun createFlashDeal(title: String, discount: Int, durationHours: Int) {
        val uid = auth.currentUser?.uid ?: return
        val expiry = Calendar.getInstance().apply {
            add(Calendar.HOUR, durationHours)
        }.time
        
        val deal = FlashDeal(
            title = title,
            discountPercentage = discount,
            expiryTime = expiry
        )
        
        _currentFlashDeal.value = deal
        firestore.collection("stylists").document(uid)
            .set(mapOf("currentFlashDeal" to deal), SetOptions.merge())
    }

    fun clearFlashDeal() {
        val uid = auth.currentUser?.uid ?: return
        _currentFlashDeal.value = null
        firestore.collection("stylists").document(uid)
            .update("currentFlashDeal", null)
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
        stylistListener?.remove()
    }
}