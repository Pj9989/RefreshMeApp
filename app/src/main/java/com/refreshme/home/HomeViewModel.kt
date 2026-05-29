package com.refreshme.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.refreshme.StyleProfile
import com.refreshme.User
import com.refreshme.aistylefinder.AiStyleRequest
import com.refreshme.aistylefinder.RecommendationEngine
import com.refreshme.data.Stylist
import com.refreshme.data.StylistCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _stylists = MutableStateFlow<List<Stylist>>(emptyList())
    val stylists: StateFlow<List<Stylist>> = _stylists.asStateFlow()

    private val _recommendedStylists = MutableStateFlow<List<Stylist>>(emptyList())
    val recommendedStylists: StateFlow<List<Stylist>> = _recommendedStylists.asStateFlow()

    private val _userName = MutableStateFlow<String>("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _styleVibe = MutableStateFlow<String?>(null)
    val styleVibe: StateFlow<String?> = _styleVibe.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Selected category filter. "all" = no filter. Otherwise one of
     * [StylistCategories.HAIR] / [StylistCategories.MAKEUP] / [StylistCategories.NAILS].
     * Driven by the All/Hair/Makeup/Nails chip row on HomeFragment.
     */
    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    /**
     * Live stylists filtered by the currently-selected category. When "all"
     * is selected this emits the full list. Pros missing a `categories`
     * field fall back to ["hair"] via the Stylist data class default so
     * legacy docs keep surfacing under the Hair filter.
     */
    val filteredStylists: StateFlow<List<Stylist>> = combine(_stylists, _selectedCategory) { list, cat ->
        if (cat == CATEGORY_ALL) list
        else list.filter { (it.categories ?: listOf(StylistCategories.HAIR)).contains(cat) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    companion object {
        const val CATEGORY_ALL = "all"
    }

    private var currentUserLocation: Location? = null
    private var allLiveStylists: List<Stylist> = emptyList()
    private var userProfile: StyleProfile? = null
    
    private var userListener: ListenerRegistration? = null
    private var stylistsListener: ListenerRegistration? = null

    init {
        observeCurrentUser()
        fetchLiveStylists()
    }

    private fun observeCurrentUser() {
        val uid = auth.currentUser?.uid ?: return
        
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                snapshot?.toObject(User::class.java)?.let { user ->
                    _userName.value = "What's good, ${user.name} 👋"
                    userProfile = user.styleProfile
                    
                    user.styleProfile?.let { profile ->
                        val formattedVibe = when(profile.vibe) {
                            "clean_classic" -> "Classic & Clean"
                            "bold_trendy" -> "Bold & Trendy"
                            "low_maintenance" -> "Low Maintenance"
                            else -> null
                        }
                        _styleVibe.value = formattedVibe
                        fetchRecommendedStylists(profile)
                    }
                }
            }
    }
    
    fun updateUserLocation(location: Location) {
        currentUserLocation = location
        recalculateStylistDistances(_stylists, allLiveStylists)
        recalculateStylistDistances(_recommendedStylists, _recommendedStylists.value)
    }

    private fun fetchLiveStylists() {
        if (auth.currentUser == null) {
            _stylists.value = emptyList()
            _isLoading.value = false
            return
        }

        stylistsListener?.remove()
        stylistsListener = db.collection("stylists")
            .whereEqualTo("availableNow", true)
            .addSnapshotListener { snapshots, e ->
                _isLoading.value = false
                if (e != null) {
                    Log.w("HomeViewModel", "Listen failed.", e)
                    _stylists.value = emptyList()
                    return@addSnapshotListener
                }

                allLiveStylists = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                }?.filter { it.isDiscoverable } ?: emptyList()
                
                recalculateStylistDistances(_stylists, allLiveStylists)
            }
    }

    private fun fetchRecommendedStylists(profile: StyleProfile) {
        viewModelScope.launch {
            try {
                val request = AiStyleRequest(profile.gender, profile.vibe, profile.frequency, profile.finish)
                val recommendations = RecommendationEngine.getRecommendations(request)
                val specialties = RecommendationEngine.getMatchingSpecialties(recommendations)

                if (specialties.isNotEmpty()) {
                    val snapshots = db.collection("stylists")
                        .whereIn("specialty", specialties)
                        .limit(10)
                        .get()
                        .await()
                        
                    val stylists = snapshots.documents.mapNotNull { doc ->
                        doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                    }.filter { it.isDiscoverable }
                    stylists.forEach { stylist ->
                        stylist.matchScore = 95
                        stylist.matchExplanation = RecommendationEngine.getMatchExplanation(stylist.specialty, request)
                    }
                    recalculateStylistDistances(_recommendedStylists, stylists)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching recommended stylists", e)
            }
        }
    }
    
    private fun recalculateStylistDistances(targetStateFlow: MutableStateFlow<List<Stylist>>, list: List<Stylist>) {
        val updatedList = list.toList()
        currentUserLocation?.let { location ->
            updatedList.forEach { stylist ->
                stylist.location?.let {
                    // Skip (0,0) placeholders — those are Flutter-side signup defaults
                    // that were never geocoded from the saved address, and produce
                    // nonsense distances like "5,900 mi away".
                    if (it.latitude == 0.0 && it.longitude == 0.0) {
                        stylist.distance = 0.0
                        return@let
                    }
                    val stylistLocation = Location("").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                    val distanceInMeters = location.distanceTo(stylistLocation)
                    val miles = (distanceInMeters / 1609.34).roundTo(1)
                    // Treat implausibly large distances as dirty data and hide.
                    stylist.distance = if (miles > 500.0) 0.0 else miles
                }
            }
            targetStateFlow.value = updatedList.sortedBy { it.distance }
        } ?: run {
            targetStateFlow.value = updatedList
        }
    }
    
    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        stylistsListener?.remove()
    }
}
