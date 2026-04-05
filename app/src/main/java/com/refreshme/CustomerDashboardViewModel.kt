package com.refreshme

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import com.refreshme.data.Stylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

data class CustomerDashboardUiState(
    val upcomingBooking: Booking? = null,
    val flashDeals: List<Stylist> = emptyList(),
    val nearbyStylists: List<Stylist> = emptyList(),
    val savedStylists: List<Stylist> = emptyList(),
    val isLoading: Boolean = false,
    val userName: String = "User"
)

@HiltViewModel
class CustomerDashboardViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDashboardUiState())
    val uiState: StateFlow<CustomerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 1. Fetch User Name
                val userDoc = firestore.collection("users").document(uid).get().await()
                val name = userDoc.getString("name") ?: "User"
                _uiState.value = _uiState.value.copy(userName = name)

                // 2. Fetch Upcoming Booking (next one within future)
                val bookingsSnapshot = firestore.collection("bookings")
                    .whereEqualTo("userId", uid)
                    .whereIn("status", listOf(BookingStatus.ACCEPTED.name, BookingStatus.DEPOSIT_PAID.name, BookingStatus.ON_THE_WAY.name))
                    .orderBy("startTime", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                val upcoming = bookingsSnapshot.documents.firstOrNull()?.let { doc ->
                    doc.toObject(Booking::class.java)?.apply { id = doc.id }
                }

                // 3. Fetch Flash Deals
                val flashSnapshot = firestore.collection("stylists")
                    .whereNotEqualTo("currentFlashDeal", null)
                    .limit(5)
                    .get()
                    .await()
                val flashDeals = flashSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                }.filter { it.hasActiveFlashDeal }

                // 4. Fetch Nearby Stylists (For now just top 10 online)
                val nearbySnapshot = firestore.collection("stylists")
                    .whereEqualTo("online", true)
                    .limit(10)
                    .get()
                    .await()
                val nearby = nearbySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                }

                // 5. Fetch Saved Stylists
                val savedSnapshot = firestore.collection("users").document(uid)
                    .collection("favorites")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                
                val savedIds = savedSnapshot.documents.map { it.id }
                val savedStylists = mutableListOf<Stylist>()
                for (id in savedIds) {
                    val doc = firestore.collection("stylists").document(id).get().await()
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)?.let { savedStylists.add(it) }
                }

                _uiState.value = _uiState.value.copy(
                    upcomingBooking = upcoming,
                    flashDeals = flashDeals,
                    nearbyStylists = nearby,
                    savedStylists = savedStylists,
                    isLoading = false
                )

            } catch (e: Exception) {
                Log.e("CustomerDashboardVM", "Error loading dashboard", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}