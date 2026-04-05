package com.refreshme.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.refreshme.User
import com.refreshme.data.Booking
import com.refreshme.data.Stylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings = _bookings.asStateFlow()

    private val _favoriteStylists = MutableStateFlow<List<Stylist>>(emptyList())
    val favoriteStylists = _favoriteStylists.asStateFlow()
    
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile = _userProfile.asStateFlow()
    
    private var userListener: ListenerRegistration? = null

    init {
        getUserData()
    }

    fun getUserData() {
        val userId = auth.currentUser?.uid ?: return
        listenToUserDoc(userId)
        getUserBookings(userId)
        getFavoriteStylists(userId)
    }
    
    private fun listenToUserDoc(userId: String) {
        userListener?.remove()
        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserProfileVM", "Error listening to user data", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    _userProfile.value = user
                }
            }
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
            firestore.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener { result ->
                    val stylists = result.documents.mapNotNull { doc ->
                        val stylist = doc.toObject(Stylist::class.java)
                        if (stylist?.id != doc.id) stylist?.copy(id = doc.id) else stylist
                    }
                    _favoriteStylists.value = stylists
                }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
