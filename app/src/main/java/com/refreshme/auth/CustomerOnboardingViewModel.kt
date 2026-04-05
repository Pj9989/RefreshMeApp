package com.refreshme.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CustomerOnboardingViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    
    private val _onboardingState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val onboardingState: StateFlow<OnboardingState> = _onboardingState.asStateFlow()

    fun saveCustomerData(name: String, email: String, photoUri: Uri?, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _onboardingState.value = OnboardingState.Error("User not logged in.")
            return
        }
        
        _onboardingState.value = OnboardingState.Loading
        
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId)

                val userData = mutableMapOf<String, Any>("email" to email)
                if (name.isNotBlank()) {
                    userData["name"] = name
                }

                if (photoUri != null) {
                    val storageRef = storage.reference.child("profile_photos/$userId.jpg")
                    storageRef.putFile(photoUri).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    userData["profileImageUrl"] = downloadUrl
                }

                // Use SetOptions.merge() so we don't accidentally overwrite the entire document
                // if it already exists with other fields like `role`.
                userDoc.set(userData, SetOptions.merge()).await()
                
                _onboardingState.value = OnboardingState.Success
                onSuccess()
            } catch (e: Exception) {
                Log.e("CustomerOnboarding", "Failed to save customer data", e)
                _onboardingState.value = OnboardingState.Error("Failed to save user data. Please try again.")
            }
        }
    }
}

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}