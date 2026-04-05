package com.refreshme.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

                // Use merge to prevent overwriting an existing user document entirely
                userDoc.set(mapOf("role" to role), SetOptions.merge()).await()
                
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