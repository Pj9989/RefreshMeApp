package com.refreshme.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.refreshme.data.Booking
import com.refreshme.data.BookingRepository
import com.refreshme.data.Review
import com.refreshme.data.Stylist
import com.refreshme.data.Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class StylistUiState {
    data class Success(val stylist: Stylist) : StylistUiState()
    data class Error(val message: String) : StylistUiState()
    object Loading : StylistUiState()
}

sealed class PhotosUiState {
    data class Success(val photoUrls: List<String>) : PhotosUiState()
    data class Error(val message: String) : PhotosUiState()
    object Loading : PhotosUiState()
}

@HiltViewModel
class StylistDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StylistUiState>(StylistUiState.Loading)
    val uiState: StateFlow<StylistUiState> = _uiState.asStateFlow()

    private val _photos = MutableStateFlow<PhotosUiState>(PhotosUiState.Loading)
    val photos: StateFlow<PhotosUiState> = _photos.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _eligibleBooking = MutableStateFlow<Booking?>(null)
    val eligibleBooking: StateFlow<Booking?> = _eligibleBooking.asStateFlow()

    private val _nextAvailableSlot = MutableStateFlow<String?>(null)
    val nextAvailableSlot: StateFlow<String?> = _nextAvailableSlot.asStateFlow()

    private val _reportSuccess = MutableStateFlow<Boolean?>(null)
    val reportSuccess: StateFlow<Boolean?> = _reportSuccess.asStateFlow()

    fun getStylist(id: String) {
        if (id.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = StylistUiState.Loading
            try {
                // 1. Fetch main profile
                val document = firestore.collection("stylists").document(id).get().await()
                
                // 2. Fetch services from sub-collection (where bundles are saved)
                val fetchedServices = try {
                    val servicesSnapshot = firestore.collection("stylists").document(id)
                        .collection("services").get().await()
                    servicesSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Service::class.java)?.apply { this.id = doc.id }
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                val baseStylist = document.toObject(Stylist::class.java)
                
                // Merge services from both sources (sub-collection takes priority)
                val allServices = if (fetchedServices.isNotEmpty()) {
                    fetchedServices
                } else {
                    baseStylist?.services ?: emptyList()
                }

                val stylist = baseStylist?.copy(
                    id = document.id,
                    services = allServices
                )
                
                if (stylist != null) {
                    _uiState.value = StylistUiState.Success(stylist)
                    _reviews.value = stylist.reviews ?: emptyList()
                    _photos.value = PhotosUiState.Success(stylist.portfolioImages ?: emptyList())
                    
                    val currentUserId = auth.currentUser?.uid
                    if (currentUserId != null) {
                        try {
                            val favDoc = firestore.collection("users").document(currentUserId)
                                .collection("favorites").document(id).get().await()
                            _isFavorite.value = favDoc.exists()
                        } catch (e: Exception) {
                            _isFavorite.value = false
                        }
                        checkRatingEligibility(id, currentUserId)
                    }

                    // Fetch AI Summary if there are reviews
                    if (_reviews.value.isNotEmpty()) {
                        fetchAiSummary(id)
                    }
                } else {
                    _uiState.value = StylistUiState.Error("Stylist not found")
                }
            } catch (e: Exception) {
                _uiState.value = StylistUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun fetchAiSummary(stylistId: String) {
        viewModelScope.launch {
            try {
                // First check if we already have a recent summary in Firestore to save tokens/cost
                val existingSummary = firestore.collection("stylists").document(stylistId)
                    .collection("aiSummary").document("current").get().await()
                
                if (existingSummary.exists()) {
                    _aiSummary.value = existingSummary.getString("summary")
                } else {
                    // Call cloud function to generate new summary
                    val data = hashMapOf("stylistId" to stylistId)
                    val result = functions.getHttpsCallable("summarizeStylistReviews")
                        .call(data)
                        .await()
                    
                    val response = result.data as? Map<*, *>
                    _aiSummary.value = response?.get("summary") as? String
                }
            } catch (e: Exception) {
                // Silently fail for AI summary
            }
        }
    }

    private suspend fun checkRatingEligibility(stylistId: String, userId: String) {
        try {
            val snapshot = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("stylistId", stylistId)
                .whereEqualTo("status", "COMPLETED")
                .whereEqualTo("isRated", false)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents[0]
                _eligibleBooking.value = Booking(
                    id = doc.id,
                    stylistId = doc.getString("stylistId") ?: "",
                    stylistName = doc.getString("stylistName") ?: ""
                )
            }
        } catch (e: Exception) {
            _eligibleBooking.value = null
        }
    }

    fun submitReview(stylistId: String, rating: Double, comment: String) {
        viewModelScope.launch {
            val booking = _eligibleBooking.value
            if (booking != null && booking.stylistId == stylistId) {
                bookingRepository.submitReview(booking, rating, comment).onSuccess {
                    _eligibleBooking.value = null
                    getStylist(stylistId)
                }
            } else {
                bookingRepository.submitDirectReview(stylistId, rating, comment).onSuccess {
                    getStylist(stylistId)
                }
            }
        }
    }

    fun toggleFavorite(id: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val isCurrentlyFavorite = _isFavorite.value
        
        viewModelScope.launch {
            try {
                val favRef = firestore.collection("users").document(currentUserId)
                    .collection("favorites").document(id)
                
                if (isCurrentlyFavorite) {
                    favRef.delete().await()
                } else {
                    favRef.set(mapOf("timestamp" to com.google.firebase.Timestamp.now())).await()
                }
                _isFavorite.value = !isCurrentlyFavorite
            } catch (e: Exception) {
            }
        }
    }

    fun reportStylist(stylistId: String, reason: String, details: String) {
        val reporterId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val reportData = mapOf(
                    "reportedUserId" to stylistId,
                    "reporterId" to reporterId,
                    "roleReported" to "STYLIST",
                    "reason" to reason,
                    "details" to details,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "PENDING_REVIEW"
                )
                
                firestore.collection("safety_reports").add(reportData).await()
                _reportSuccess.value = true
            } catch (e: Exception) {
                _reportSuccess.value = false
            }
        }
    }
    
    fun resetReportStatus() {
        _reportSuccess.value = null
    }
}