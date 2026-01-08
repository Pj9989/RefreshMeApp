package com.refreshme.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    init {
        fetchBookings()
    }

    private fun fetchBookings() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                try {
                    val snapshot = firestore.collection("users").document(userId)
                        .collection("bookings")
                        .get()
                        .await()
                    val bookingList = snapshot.toObjects(Booking::class.java)
                    _bookings.value = bookingList
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
}
