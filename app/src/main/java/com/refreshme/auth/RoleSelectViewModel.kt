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

class RoleSelectViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private val _roleSelectState = MutableStateFlow<RoleSelectState>(RoleSelectState.Idle)
    val roleSelectState: StateFlow<RoleSelectState> = _roleSelectState.asStateFlow()

    fun onRoleSelected(role: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _roleSelectState.value = RoleSelectState.Error("User not logged in.")
            return
        }

        _roleSelectState.value = RoleSelectState.Loading
        
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(userId)
                val firebaseUser = auth.currentUser
                val displayName = firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: firebaseUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                    ?: "RefreshMe Pro"

                // Use merge to prevent overwriting an existing user document entirely
                userDoc.set(mapOf("role" to role), SetOptions.merge()).await()

                if (role == "STYLIST") {
                    firestore.collection("stylists").document(userId).set(
                        mapOf(
                            "name" to displayName,
                            "email" to (firebaseUser?.email ?: ""),
                            "role" to role,
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
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                } else if (role == "SALON_OWNER") {
                    val existingUser = userDoc.get().await()
                    val shopId = existingUser.getString("shopId")
                        ?: firestore.collection("shops").document().id
                    val shopName = if (displayName.endsWith("Salon", ignoreCase = true) ||
                        displayName.endsWith("Studio", ignoreCase = true)
                    ) {
                        displayName
                    } else {
                        "$displayName Studio"
                    }

                    firestore.collection("shops").document(shopId).set(
                        mapOf(
                            "ownerId" to userId,
                            "name" to shopName,
                            "bio" to "",
                            "address" to "",
                            "phone" to "",
                            "website" to "",
                            "stylistIds" to listOf(userId),
                            "isPublic" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()

                    userDoc.set(
                        mapOf(
                            "shopId" to shopId,
                            "businessName" to shopName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                }
                
                _roleSelectState.value = RoleSelectState.Success
                onSuccess()
            } catch (e: Exception) {
                Log.e("RoleSelect", "Failed to save user role", e)
                _roleSelectState.value = RoleSelectState.Error("Failed to save role. Please try again.")
            }
        }
    }
}

sealed class RoleSelectState {
    object Idle : RoleSelectState()
    object Loading : RoleSelectState()
    object Success : RoleSelectState()
    data class Error(val message: String) : RoleSelectState()
}
