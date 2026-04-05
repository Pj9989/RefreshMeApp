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
import kotlinx.coroutines.tasks.await
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
        // Only start listening if user is signed in
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                getUserData()
            } else {
                // User signed out, clean up listener and clear data
                userListener?.remove()
                userListener = null
                _userProfile.value = null
                _bookings.value = emptyList()
                _favoriteStylists.value = emptyList()
            }
        }
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
                    // Ignore PERMISSION_DENIED errors when signing out
                    if (error.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.e("UserProfileVM", "Error listening to user data", error)
                    }
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
            try {
                firestore.collection("users").document(userId).collection("bookings")
                    .get()
                    .addOnSuccessListener { result ->
                        _bookings.value = result.toObjects(Booking::class.java)
                    }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun getFavoriteStylists(userId: String) {
        viewModelScope.launch {
            try {
                val favResult = firestore.collection("users").document(userId)
                    .collection("favorites")
                    .get()
                    .await()
                
                val stylistIds = favResult.documents.map { it.id }
                
                if (stylistIds.isEmpty()) {
                    _favoriteStylists.value = emptyList()
                    return@launch
                }
                
                val fetchedStylists = mutableListOf<Stylist>()
                for (id in stylistIds) {
                    try {
                        val stylistDoc = firestore.collection("stylists").document(id).get().await()
                        val stylist = stylistDoc.toObject(Stylist::class.java)?.copy(id = stylistDoc.id)
                        if (stylist != null) {
                            fetchedStylists.add(stylist)
                        }
                    } catch (e: Exception) {
                        // Skip if stylist doc missing/deleted
                    }
                }
                
                _favoriteStylists.value = fetchedStylists
            } catch (e: Exception) {
                Log.e("UserProfileVM", "Error fetching favorites", e)
            }
        }
    }
    
    fun removeFavorite(stylistId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("favorites").document(stylistId).delete().await()
                // Update local list
                val currentFavs = _favoriteStylists.value.toMutableList()
                currentFavs.removeAll { it.id == stylistId }
                _favoriteStylists.value = currentFavs
            } catch (e: Exception) {
                Log.e("UserProfileVM", "Error removing favorite", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}