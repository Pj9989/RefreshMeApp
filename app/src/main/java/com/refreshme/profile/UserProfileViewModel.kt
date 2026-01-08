package com.refreshme.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Booking
import com.refreshme.data.Stylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings = _bookings.asStateFlow()

    private val _favoriteStylists = MutableStateFlow<List<Stylist>>(emptyList())
    val favoriteStylists = _favoriteStylists.asStateFlow()

    fun getUserData() {
        val userId = auth.currentUser?.uid ?: return
        getUserBookings(userId)
        getFavoriteStylists(userId)
    }

    private fun getUserBookings(userId: String) {
        viewModelScope.launch {
            firestore.collection("users").document(userId).collection("bookings")
                .get()
                .addOnSuccessListener { result ->
                    _bookings.value = result.toObjects(Booking::class.java)
                }
        }
    }

    private fun getFavoriteStylists(userId: String) {
        viewModelScope.launch {
            // Assuming favorite stylists are stored in a "favorites" subcollection
            firestore.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener { result ->
                    // This assumes the documents in "favorites" contain stylist data or IDs
                    // For this example, we'll assume they are Stylist objects
                    _favoriteStylists.value = result.toObjects(Stylist::class.java)
                }
        }
    }
}
