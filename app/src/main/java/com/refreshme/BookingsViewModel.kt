package com.refreshme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.data.Booking
import com.refreshme.data.BookingRepository
import com.refreshme.data.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class BookingsUiState {
    object Loading : BookingsUiState()
    data class Success(val bookings: List<Booking>) : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private val _showRatingDialog = MutableStateFlow<Booking?>(null)
    val showRatingDialog: StateFlow<Booking?> = _showRatingDialog.asStateFlow()

    init {
        observeBookings()
    }

    private fun observeBookings() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = BookingsUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            repository.getCustomerBookings(uid)
                .catch { e ->
                    _uiState.value = BookingsUiState.Error("Failed to load bookings: ${e.localizedMessage}")
                }
                .collect { bookings ->
                    _uiState.value = BookingsUiState.Success(bookings)
                }
        }
    }

    fun openRatingDialog(booking: Booking) {
        _showRatingDialog.value = booking
    }

    fun closeRatingDialog() {
        _showRatingDialog.value = null
    }

    fun submitReview(booking: Booking, rating: Double, comment: String) {
        viewModelScope.launch {
            repository.submitReview(booking, rating, comment)
            closeRatingDialog()
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                val data = hashMapOf("bookingId" to bookingId)
                functions.getHttpsCallable("cancelBooking")
                    .call(data)
                    .await()
            } catch (e: Exception) {
                repository.updateBookingStatus(bookingId, BookingStatus.CANCELLED)
            }
        }
    }
    
    fun rescheduleBooking(bookingId: String, newDate: java.util.Date) {
        viewModelScope.launch {
            try {
                repository.rescheduleBooking(bookingId, com.google.firebase.Timestamp(newDate))
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }
}