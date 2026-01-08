package com.refreshme.stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StylistMainViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    // TODO: Fetch stylist's initial online status from Firestore.

    fun setOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            firestore.collection("stylists").document(userId)
                .update("isOnline", isOnline)
                .addOnSuccessListener {
                    _isOnline.value = isOnline
                }
                .addOnFailureListener {
                    // TODO: Handle failure to update status.
                }
        }
    }
}