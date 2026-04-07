package com.refreshme.stylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.refreshme.data.Service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class ServicesUiState {
    object Loading : ServicesUiState()
    data class Success(val services: List<Service>) : ServicesUiState()
    data class Error(val message: String) : ServicesUiState()
}

@HiltViewModel
class ManageServicesViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private var servicesListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow<ServicesUiState>(ServicesUiState.Loading)
    val uiState: StateFlow<ServicesUiState> = _uiState

    init {
        loadServices()
    }

    fun loadServices() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = ServicesUiState.Loading
        
        servicesListener?.remove()
        servicesListener = firestore.collection("stylists").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = ServicesUiState.Error(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }
                
                val stylist = snapshot?.toObject(com.refreshme.data.Stylist::class.java)
                val services = stylist?.services ?: emptyList()
                
                _uiState.value = ServicesUiState.Success(services)
            }
    }

    fun addService(service: Service) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newService = service.copy(id = java.util.UUID.randomUUID().toString())
                firestore.collection("stylists").document(uid)
                    .update("services", com.google.firebase.firestore.FieldValue.arrayUnion(newService)).await()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateService(service: Service) {
        val uid = auth.currentUser?.uid ?: return
        if (service.id.isBlank()) return
        
        viewModelScope.launch {
            try {
                val doc = firestore.collection("stylists").document(uid).get().await()
                val stylist = doc.toObject(com.refreshme.data.Stylist::class.java)
                val currentServices = stylist?.services?.toMutableList() ?: mutableListOf()
                
                val index = currentServices.indexOfFirst { it.id == service.id }
                if (index != -1) {
                    currentServices[index] = service
                    firestore.collection("stylists").document(uid)
                        .update("services", currentServices).await()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteService(serviceId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("stylists").document(uid).get().await()
                val stylist = doc.toObject(com.refreshme.data.Stylist::class.java)
                val currentServices = stylist?.services?.toMutableList() ?: mutableListOf()
                
                val serviceToRemove = currentServices.find { it.id == serviceId }
                if (serviceToRemove != null) {
                    firestore.collection("stylists").document(uid)
                        .update("services", com.google.firebase.firestore.FieldValue.arrayRemove(serviceToRemove)).await()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        servicesListener?.remove()
    }
}
