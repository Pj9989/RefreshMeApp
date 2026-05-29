package com.refreshme.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StylistOnboardingViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private val _onboardingState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    fun saveStylistData(
        fullName: String,
        businessName: String,
        onSuccess: () -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _onboardingState.value = OnboardingState.Error("User not logged in.")
            return
        }

        _onboardingState.value = OnboardingState.Loading

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId)

                val userData = mutableMapOf<String, Any>(
                    "name" to fullName,
                    "fullName" to fullName,
                    "businessName" to businessName,
                    "role" to "STYLIST"
                )

                userDoc.set(userData, SetOptions.merge()).await()

                firestore.collection("stylists").document(userId).set(
                    mapOf(
                        "name" to fullName,
                        "businessName" to businessName,
                        "role" to "STYLIST",
                        "online" to false,
                        "availableNow" to false,
                        "available" to false,
                        "subscriptionActive" to false,
                        "services" to emptyList<Map<String, Any>>(),
                        "categories" to listOf("hair"),
                        "servesGender" to listOf("Men", "Women", "Non-binary"),
                        "serviceLocationType" to "mobile",
                        "offersAtHomeService" to true,
                        "atHomeServiceFee" to 20.0,
                        "maxTravelRangeKm" to 15,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
                
                _onboardingState.value = OnboardingState.Success
                onSuccess()
            } catch (e: Exception) {
                Log.e("StylistOnboarding", "Failed to save stylist data", e)
                _onboardingState.value = OnboardingState.Error("Failed to save data. Please try again.")
            }
        }
    }
}
