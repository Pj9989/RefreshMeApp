package com.refreshme.stylist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.refreshme.data.Stylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class StylistHomeViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private val _stylist = MutableStateFlow<Stylist?>(null)
    val stylist: StateFlow<Stylist?> = _stylist.asStateFlow()

    private var stylistListener: ListenerRegistration? = null

    init {
        observeStylistData()
    }

    private fun observeStylistData() {
        val uid = auth.currentUser?.uid ?: return
        
        stylistListener?.remove()
        stylistListener = firestore.collection("stylists").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StylistHomeVM", "Error listening to stylist data", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val stylistData = snapshot.toObject(Stylist::class.java)?.copy(id = uid)
                    _stylist.value = stylistData
                    _isOnline.value = stylistData?.isOnline ?: false
                }
            }
    }

    fun toggleOnlineStatus() {
        val uid = auth.currentUser?.uid ?: return
        val newStatus = !_isOnline.value
        
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "online" to newStatus,
                    "availableNow" to newStatus,
                    "lastOnlineAt" to FieldValue.serverTimestamp()
                )
                
                firestore.collection("stylists").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e("StylistHomeVM", "Failed to toggle online status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stylistListener?.remove()
    }
}
