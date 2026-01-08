package com.refreshme.details

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.refreshme.data.Review
import com.refreshme.data.Stylist
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

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

class StylistDetailViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private val _uiState = MutableStateFlow<StylistUiState>(StylistUiState.Loading)
    val uiState: StateFlow<StylistUiState> = _uiState.asStateFlow()

    private val _paymentClientSecret = MutableStateFlow<String?>(null)
    val paymentClientSecret: StateFlow<String?> = _paymentClientSecret.asStateFlow()

    private val _photos = MutableStateFlow<PhotosUiState>(PhotosUiState.Loading)
    val photos: StateFlow<PhotosUiState> = _photos.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    fun getStylist(stylistId: String) {
        viewModelScope.launch {
            _uiState.value = StylistUiState.Loading
            try {
                val document = firestore.collection("stylists").document(stylistId).get().await()
                val stylist = document.toObject(Stylist::class.java)
                if (stylist != null) {
                    _uiState.value = StylistUiState.Success(stylist.copy(id = document.id))
                    getPhotos(stylistId)
                    getReviews(stylistId)
                } else {
                    _uiState.value = StylistUiState.Error("Stylist not found")
                }
            } catch (e: Exception) {
                _uiState.value = StylistUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getPhotos(stylistId: String) {
        viewModelScope.launch {
            _photos.value = PhotosUiState.Loading
            try {
                val listResult = storage.reference.child("stylists/$stylistId").listAll().await()
                val urls = listResult.items.map { it.downloadUrl.await().toString() }
                _photos.value = PhotosUiState.Success(urls)
            } catch (e: Exception) {
                Log.e("StylistDetailViewModel", "Error getting photos", e)
                _photos.value = PhotosUiState.Error(e.message ?: "Error getting photos")
            }
        }
    }

    private fun getReviews(stylistId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("reviews")
                    .whereEqualTo("stylistId", stylistId)
                    .get()
                    .await()
                val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
                _reviews.value = reviews
            } catch (e: Exception) {
                Log.e("StylistDetailViewModel", "Error getting reviews", e)
            }
        }
    }

    fun bookAppointment(
        stylistId: String,
        date: Date,
        amount: Double,
        currency: String,
        stripeAccountId: String
    ) {
        // TODO: Create a cloud function to create a payment intent
    }

    fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        // TODO: Handle payment result
    }

    fun addReview(stylistId: String, rating: Int, text: String) {
        // TODO: Add review to firestore
    }

    fun uploadPhoto(stylistId: String, uri: Uri) {
        // TODO: Upload photo to storage
    }
}