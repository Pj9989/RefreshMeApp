package com.refreshme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                val name = runCatching {
                    firestore.collection("users").document(uid).get().await().getString("name")
                }.getOrNull() ?: "User"
                _uiState.value = _uiState.value.copy(userName = name)

                // 2. Fetch Upcoming Booking (next one within future)
                val upcoming = runCatching {
                    val bookingsSnapshot = firestore.collection("bookings")
                        .whereEqualTo("userId", uid)
                        .whereIn("status", listOf(BookingStatus.ACCEPTED.name, BookingStatus.DEPOSIT_PAID.name, BookingStatus.ON_THE_WAY.name))
                        .orderBy("startTime", Query.Direction.ASCENDING)
                        .limit(1)
                        .get()
                        .await()

                    bookingsSnapshot.documents.firstOrNull()?.let { doc ->
                        doc.toObject(Booking::class.java)?.apply { id = doc.id }
                    }
                }.onFailure {
                    Log.w("CustomerDashboardVM", "Unable to load upcoming booking", it)
                }.getOrNull()

                // 3. Fetch Flash Deals
                val flashDeals = runCatching {
                    firestore.collection("stylists")
                        .whereNotEqualTo("currentFlashDeal", null)
                        .limit(8)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { doc -> doc.toObject(Stylist::class.java)?.copy(id = doc.id) }
                        .filter { it.shouldShowOnCustomerDashboard && it.hasActiveFlashDeal }
                }.onFailure {
                    Log.w("CustomerDashboardVM", "Unable to load flash deals", it)
                }.getOrElse { emptyList() }

                // 4. Fetch customer-visible stylists. Some profiles use `online`,
                // others use `availableNow`; loading both prevents an empty home
                // screen when only one availability field is populated.
                val nearby = loadCustomerVisibleStylists()

                // 5. Fetch Saved Stylists
                val savedStylists = runCatching {
                    val savedSnapshot = firestore.collection("users").document(uid)
                        .collection("favorites")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(5)
                        .get()
                        .await()

                    savedSnapshot.documents.mapNotNull { favorite ->
                        val doc = firestore.collection("stylists").document(favorite.id).get().await()
                        doc.toObject(Stylist::class.java)
                            ?.copy(id = doc.id)
                            ?.takeIf { it.shouldShowOnCustomerDashboard }
                    }
                }.onFailure {
                    Log.w("CustomerDashboardVM", "Unable to load saved stylists", it)
                }.getOrElse { emptyList() }

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

    private suspend fun loadCustomerVisibleStylists(): List<Stylist> {
        val selected = linkedMapOf<String, Stylist>()

        suspend fun addQuery(field: String, value: Any, limit: Long = 20) {
            runCatching {
                firestore.collection("stylists")
                    .whereEqualTo(field, value)
                    .limit(limit)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> doc.toObject(Stylist::class.java)?.copy(id = doc.id) }
                    .forEach { stylist ->
                        if (stylist.shouldShowOnCustomerDashboard) {
                            selected[stylist.id.ifBlank { stylist.name }] = stylist
                        }
                    }
            }.onFailure {
                Log.w("CustomerDashboardVM", "Unable to load stylists where $field == $value", it)
            }
        }

        addQuery("availableNow", true)
        addQuery("online", true)
        addQuery("verified", true)
        addQuery("isVerified", true)
        addQuery("featured", true)

        if (selected.isEmpty()) {
            runCatching {
                firestore.collection("stylists")
                    .limit(40)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> doc.toObject(Stylist::class.java)?.copy(id = doc.id) }
                    .forEach { stylist ->
                        if (stylist.shouldShowOnCustomerDashboard) {
                            selected[stylist.id.ifBlank { stylist.name }] = stylist
                        }
                    }
            }.onFailure {
                Log.w("CustomerDashboardVM", "Unable to load fallback stylists", it)
            }
        }

        return selected.values
            .sortedWith(
                compareByDescending<Stylist> { it.isOnline == true || it.isCurrentlyAvailable }
                    .thenByDescending { it.isFeatured == true }
                    .thenByDescending { it.rating }
                    .thenBy { it.name }
            )
            .take(20)
    }

    private val Stylist.shouldShowOnCustomerDashboard: Boolean
        get() = isVerifiedStylist && hasPublicProfileSetup
}
