package com.refreshme.stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
                    firestore.collection("stylists").document(uid)
                        .set(mapOf("online" to newStatus), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    _isOnline.value = newStatus
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
}