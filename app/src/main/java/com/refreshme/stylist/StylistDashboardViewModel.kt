package com.refreshme.stylist

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StylistDashboardViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified

    private val _profileComplete = MutableStateFlow(false)
    val profileComplete: StateFlow<Boolean> = _profileComplete

    init {
        checkVerificationAndProfileStatus()
    }

    private fun checkVerificationAndProfileStatus() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("stylists").document(userId)
            .addSnapshotListener { snapshot, _ ->
                _isVerified.value = snapshot?.getBoolean("isVerified") ?: false
                // TODO: Add logic to check for profile completeness
                _profileComplete.value = true
            }
    }

    fun toggleAvailability() {
        if (_isVerified.value && _profileComplete.value) {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("stylists").document(userId)
                .update("isAvailable", !_isAvailable.value)
        }
    }
}