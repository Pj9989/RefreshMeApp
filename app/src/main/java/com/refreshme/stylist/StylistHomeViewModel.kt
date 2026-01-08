package com.refreshme.stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StylistHomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        listenForOnlineStatus()
    }

    private fun listenForOnlineStatus() {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("stylists").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    _isOnline.value = snapshot?.getBoolean("online") ?: false
                }
        }
    }

    fun toggleOnlineStatus() {
        viewModelScope.launch {
            auth.currentUser?.uid?.let { uid ->
                val newStatus = !_isOnline.value
                try {
                    val updates = hashMapOf<String, Any>(
                        "online" to newStatus,
                        "availableNow" to newStatus,
                        "lastOnlineAt" to FieldValue.serverTimestamp()
                    )
                    
                    firestore.collection("stylists").document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    _isOnline.value = newStatus
                } catch (e: Exception) {
                    // Handle error - could add error state here
                    android.util.Log.e("StylistHomeVM", "Failed to toggle online status", e)
                }
            }
        }
    }

    /**
     * Call this when the app goes to background or is destroyed
     * to set the stylist offline automatically
     */
    fun setOffline() {
        viewModelScope.launch {
            auth.currentUser?.uid?.let { uid ->
                try {
                    val updates = hashMapOf<String, Any>(
                        "online" to false,
                        "availableNow" to false,
                        "lastOnlineAt" to FieldValue.serverTimestamp()
                    )
                    
                    firestore.collection("stylists").document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    android.util.Log.e("StylistHomeVM", "Failed to set offline", e)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Optionally set offline when ViewModel is cleared
        // Note: This might not always fire when app is killed
    }
}
