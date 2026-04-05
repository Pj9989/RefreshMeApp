package com.refreshme.stylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.refreshme.data.Stylist
import com.refreshme.data.StylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class StylistProfileUiState(
    val stylist: Stylist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isTrialActive: Boolean = false,
    val daysLeftInTrial: Long = 0,
    val isSubscriptionActive: Boolean = false,
    val hasActiveSubscriptionOrTrial: Boolean = false
)

@HiltViewModel
class StylistProfileViewModel @Inject constructor(
    private val repository: StylistRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(StylistProfileUiState())
    val uiState: StateFlow<StylistProfileUiState> = _uiState.asStateFlow()

    private val TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(30)
    private var stylistListener: ListenerRegistration? = null

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            observeStylistData(uid)
        }
    }

    private fun observeStylistData(uid: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        stylistListener?.remove()
        stylistListener = firestore.collection("stylists").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.localizedMessage)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val stylist = snapshot.toObject(Stylist::class.java)?.copy(id = uid)
                    if (stylist != null) {
                        val isSubActive = stylist.isSubscriptionActive
                        val trialStartTimeMillis = stylist.trialStartTime?.time ?: 0L
                        
                        val timeElapsed = System.currentTimeMillis() - trialStartTimeMillis
                        val isTrialActive = trialStartTimeMillis > 0 && timeElapsed < TRIAL_DURATION_MS
                        val daysLeft = if (isTrialActive) TimeUnit.MILLISECONDS.toDays(TRIAL_DURATION_MS - timeElapsed) else 0

                        _uiState.value = _uiState.value.copy(
                            stylist = stylist,
                            isLoading = false,
                            isSubscriptionActive = isSubActive,
                            isTrialActive = isTrialActive,
                            daysLeftInTrial = daysLeft,
                            hasActiveSubscriptionOrTrial = isSubActive || isTrialActive,
                            error = null
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Stylist profile not found")
                }
            }
    }

    fun toggleOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                firestore.collection("stylists").document(uid)
                    .update(mapOf("online" to online, "availableNow" to online))
            } catch (e: Exception) {
                Log.e("StylistProfileVM", "Failed to toggle status", e)
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        stylistListener?.remove()
    }
}
