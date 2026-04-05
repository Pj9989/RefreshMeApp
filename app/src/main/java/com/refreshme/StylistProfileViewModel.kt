package com.refreshme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(StylistProfileUiState())
    val uiState: StateFlow<StylistProfileUiState> = _uiState.asStateFlow()

    private val TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(30)

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            loadStylistData(uid)
        }
    }

    fun loadStylistData(uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = repository.getStylist(uid)
            if (result.isSuccess) {
                val stylist = result.getOrNull()
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
                        hasActiveSubscriptionOrTrial = isSubActive || isTrialActive
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Stylist not found")
                }
            } else {
                val e = result.exceptionOrNull()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e?.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun fetchStylist(stylistId: String) {
        loadStylistData(stylistId)
    }

    fun signOut() {
        auth.signOut()
    }
}