package com.refreshme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Booking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    init {
        fetchBookings()
    }

    private fun fetchBookings() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val snapshot = firestore.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                _bookings.value = snapshot.toObjects(Booking::class.java)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}