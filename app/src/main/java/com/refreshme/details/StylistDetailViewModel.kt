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

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _eligibleBooking = MutableStateFlow<Booking?>(null)
    val eligibleBooking: StateFlow<Booking?> = _eligibleBooking.asStateFlow()

    private val _nextAvailableSlot = MutableStateFlow<String?>(null)
    val nextAvailableSlot: StateFlow<String?> = _nextAvailableSlot.asStateFlow()

    fun getStylist(id: String) {
        if (id.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = StylistUiState.Loading
            try {
                val document = firestore.collection("stylists").document(id).get().await()
                val stylist = document.toObject(Stylist::class.java)?.copy(id = document.id)
                
                if (stylist != null) {
                    _uiState.value = StylistUiState.Success(stylist)
                    _reviews.value = stylist.reviews ?: emptyList()
                    _photos.value = PhotosUiState.Success(stylist.portfolioImages ?: emptyList())
                    
                    val currentUserId = auth.currentUser?.uid
                    if (currentUserId != null) {
                        // Check if favorite
                        val favDoc = firestore.collection("users").document(currentUserId)
                            .collection("favorites").document(id).get().await()
                        _isFavorite.value = favDoc.exists()
                        
                        // Try to find a booking to mark as rated, but we'll allow reviewing anyway
                        checkRatingEligibility(id, currentUserId)
                    }
                } else {
                    _uiState.value = StylistUiState.Error("Stylist not found")
                }
            } catch (e: Exception) {
                _uiState.value = StylistUiState.Error(e.localizedMessage ?: "Unknown error")
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
                // Submit review tied to a specific booking
                bookingRepository.submitReview(booking, rating, comment).onSuccess {
                    _eligibleBooking.value = null
                    getStylist(stylistId)
                }
            } else {
                // Submit review directly to stylist profile (Fallback for testing/direct reviews)
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
                // Handle error
            }
        }
    }
}
