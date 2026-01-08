package com.refreshme.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Service
import com.refreshme.data.Stylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class NewBookingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _stylist = MutableStateFlow<Stylist?>(null)
    val stylist: StateFlow<Stylist?> = _stylist

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService: StateFlow<Service?> = _selectedService

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState

    fun fetchStylist(stylistId: String) {
        viewModelScope.launch {
            db.collection("stylists").document(stylistId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        _stylist.value = document.toObject(Stylist::class.java)
                    }
                }
        }
    }

    fun selectService(service: Service) {
        _selectedService.value = service
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
    }

    fun createBooking() {
        viewModelScope.launch {
            val stylistValue = _stylist.value
            val serviceValue = _selectedService.value
            val dateValue = _selectedDate.value
            val currentUser = auth.currentUser

            if (stylistValue != null && serviceValue != null && dateValue != null && currentUser != null) {
                _bookingState.value = BookingState.Loading
                val booking = Booking(
                    stylistId = stylistValue.id,
                    stylistName = stylistValue.name,
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "Unknown",
                    service = serviceValue,
                    dateTime = dateValue,
                    status = "Pending"
                )

                db.collection("users").document(currentUser.uid).collection("bookings").add(booking)
                    .addOnSuccessListener {
                        _bookingState.value = BookingState.Success
                    }
                    .addOnFailureListener {
                        _bookingState.value = BookingState.Error("Failed to create booking.")
                    }
            } else {
                _bookingState.value = BookingState.Error("Please select a service and a date.")
            }
        }
    }
}

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    object Success : BookingState()
    data class Error(val message: String) : BookingState()
}
