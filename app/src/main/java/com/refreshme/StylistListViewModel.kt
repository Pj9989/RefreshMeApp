package com.refreshme

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.data.Stylist
import com.refreshme.data.StylistRepository
import com.refreshme.data.ServiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StylistListViewModel @Inject constructor(
    private val repository: StylistRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _allStylists = MutableStateFlow<List<Stylist>>(emptyList())
    private val _filteredStylists = MutableStateFlow<List<Stylist>>(emptyList())
    val stylists: StateFlow<List<Stylist>> = _filteredStylists.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _specialties = MutableStateFlow<List<String>>(emptyList())
    val specialties = _specialties.asStateFlow()

    private val _specialtyFilters = MutableStateFlow<List<String>>(emptyList())
    val specialtyFilters = _specialtyFilters.asStateFlow()

    private val _vibeFilters = MutableStateFlow<List<String>>(emptyList())
    val vibeFilters = _vibeFilters.asStateFlow()

    private val _faceShapeFilter = MutableStateFlow<String?>(null)
    val faceShapeFilter = _faceShapeFilter.asStateFlow()

    private val _hasFlashDealFilter = MutableStateFlow(false)
    val hasFlashDealFilter = _hasFlashDealFilter.asStateFlow()

    private val _ratingFilter = MutableStateFlow(0f)
    val ratingFilter = _ratingFilter.asStateFlow()

    private val _priceFilter = MutableStateFlow(1000f)
    val priceFilter = _priceFilter.asStateFlow()

    private val _atHomeService = MutableStateFlow(false)
    val atHomeService = _atHomeService.asStateFlow()

    private val _lateNightService = MutableStateFlow(false)
    val lateNightService = _lateNightService.asStateFlow()

    init {
        loadAllStylists()
    }

    fun loadAllStylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getVerifiedStylists().onSuccess { list ->
                _allStylists.value = list
                // Extract unique specialties
                _specialties.value = list.mapNotNull { it.specialty }.distinct().sorted()
                applyFilters()
            }.onFailure { e ->
                _error.value = "Failed to load stylists: ${e.localizedMessage}"
            }
            
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    fun setUserLocation(latLng: LatLng) {
        _userLocation.value = latLng
        applyFilters()
    }

    fun filterBySpecialties(specialties: List<String>) {
        _specialtyFilters.value = specialties
        applyFilters()
    }

    fun filterByVibes(vibes: List<String>) {
        _vibeFilters.value = vibes
        applyFilters()
    }

    fun setFaceShapeFilter(shape: String?) {
        _faceShapeFilter.value = shape
        applyFilters()
    }

    fun setFlashDealFilter(enabled: Boolean) {
        _hasFlashDealFilter.value = enabled
        applyFilters()
    }

    fun setRatingFilter(rating: Float) {
        _ratingFilter.value = rating
        applyFilters()
    }

    fun setPriceFilter(price: Float) {
        _priceFilter.value = price
        applyFilters()
    }

    fun setAtHomeService(enabled: Boolean) {
        _atHomeService.value = enabled
        applyFilters()
    }

    fun setLateNightService(enabled: Boolean) {
        _lateNightService.value = enabled
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = _allStylists.value
        val userLoc = _userLocation.value

        // Apply Search
        val query = _searchQuery.value.lowercase()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { stylist ->
                stylist.name.lowercase().contains(query) ||
                stylist.address?.lowercase()?.contains(query) == true ||
                stylist.specialty?.lowercase()?.contains(query) == true ||
                stylist.vibes?.any { it.lowercase().contains(query) } == true
            }
        }

        // Apply Specialty Filter
        val selectedSpecialties = _specialtyFilters.value
        if (selectedSpecialties.isNotEmpty()) {
            filtered = filtered.filter { stylist ->
                val stylistSpecialty = stylist.specialty?.lowercase() ?: ""
                val stylistBio = stylist.bio?.lowercase() ?: ""

                selectedSpecialties.any { filter ->
                    if (filter.equals(stylist.specialty, ignoreCase = true)) return@any true
                    
                    val keywords = getKeywordsForTag(filter)
                    keywords.any { keyword ->
                        stylistSpecialty.contains(keyword) || stylistBio.contains(keyword)
                    }
                }
            }
        }

        // Apply Vibe Filter
        val selectedVibes = _vibeFilters.value
        if (selectedVibes.isNotEmpty()) {
            filtered = filtered.filter { stylist ->
                stylist.vibes?.any { vibe -> 
                    selectedVibes.any { filter -> filter.equals(vibe, ignoreCase = true) }
                } == true
            }
        }

        // Apply Flash Deal Filter
        if (_hasFlashDealFilter.value) {
            filtered = filtered.filter { it.hasActiveFlashDeal }
        }

        // Calculate Match Scores for Face Shape
        val faceShape = _faceShapeFilter.value
        if (faceShape != null) {
            filtered.forEach { stylist ->
                if (stylist.recommendedFaceShapes?.contains(faceShape) == true) {
                    stylist.matchScore = 95 // High match
                } else {
                    stylist.matchScore = 0
                }
            }
        } else {
            // Reset scores if no filter
            filtered.forEach { it.matchScore = 0 }
        }

        // Apply Rating Filter
        val minRating = _ratingFilter.value
        if (minRating > 0) {
            filtered = filtered.filter { it.rating >= minRating }
        }

        // Apply Price Filter
        val maxPrice = _priceFilter.value
        if (maxPrice < 1000f) {
            filtered = filtered.filter { stylist ->
                stylist.services?.any { it.price <= maxPrice } ?: true
            }
        }

        // Apply At-Home Service Filter & Travel Radius Check
        if (_atHomeService.value) {
            filtered = filtered.filter { stylist ->
                if (stylist.offersAtHomeService != true) return@filter false
                
                // If we have user location, only show if within travel range
                if (userLoc != null && stylist.location != null) {
                    val distanceKm = calculateDistanceKm(userLoc, LatLng(stylist.location.latitude, stylist.location.longitude))
                    distanceKm <= (stylist.maxTravelRangeKm ?: 15)
                } else {
                    true // Show anyway if location unknown
                }
            }
        }

        // Apply 24/7 / Late Night Filter
        if (_lateNightService.value) {
            filtered = filtered.filter { stylist ->
                stylist.serviceType == ServiceType.ALL_HOURS || stylist.serviceType == ServiceType.AFTER_HOURS
            }
        }

        // Sort Logic
        _filteredStylists.value = filtered.sortedWith { s1, s2 ->
            // 1. Match Score
            if (s1.matchScore != s2.matchScore) return@sortedWith s2.matchScore.compareTo(s1.matchScore)
            
            // 2. Proximity
            if (userLoc != null) {
                val d1 = s1.location?.let { calculateDistanceKm(userLoc, LatLng(it.latitude, it.longitude)) } ?: Double.MAX_VALUE
                val d2 = s2.location?.let { calculateDistanceKm(userLoc, LatLng(it.latitude, it.longitude)) } ?: Double.MAX_VALUE
                if (Math.abs(d1 - d2) > 0.8) return@sortedWith d1.compareTo(d2) // Increased threshold to approx 0.5 miles
            }
            
            // 3. Flash Deal
            if (s1.hasActiveFlashDeal != s2.hasActiveFlashDeal) return@sortedWith s2.hasActiveFlashDeal.compareTo(s1.hasActiveFlashDeal)
            
            // 4. Online Status
            val online1 = s1.isOnline == true
            val online2 = s2.isOnline == true
            if (online1 != online2) return@sortedWith online2.compareTo(online1)
            
            // 5. Rating
            s2.rating.compareTo(s1.rating)
        }
    }
    
    private fun calculateDistanceKm(loc1: LatLng, loc2: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
        return results[0].toDouble() / 1000.0 // Keep in KM for internal range checks
    }
    
    private fun getKeywordsForTag(tag: String): List<String> {
        return when(tag) {
            "classic_cuts" -> listOf("classic", "cut", "barber", "gentleman", "traditional")
            "fades" -> listOf("fade", "taper", "sharp", "blend", "barber")
            "line_ups" -> listOf("line", "edge", "shape", "razor")
            "trendy_styles" -> listOf("trend", "modern", "crop", "texture", "fashion")
            "long_hair" -> listOf("long", "layer", "bob", "style", "scissor")
            "buzz_cuts" -> listOf("buzz", "clipper", "military")
            "texture_work" -> listOf("texture", "perm", "wave", "curl")
            
            // Diverse new keywords
            "silk_press" -> listOf("silk", "press", "blowout", "straight", "natural")
            "wig_installs" -> listOf("wig", "install", "lace", "frontal", "closure")
            "balayage" -> listOf("balayage", "color", "blonde", "highlight")
            "bridal" -> listOf("bridal", "wedding", "updo", "event")
            "extensions" -> listOf("extension", "sew-in", "tape-in", "micro", "links")
            "lash_tech" -> listOf("lash", "extension", "volume", "classic")
            else -> listOf(tag.replace("_", " ").lowercase())
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _specialtyFilters.value = emptyList()
        _vibeFilters.value = emptyList()
        _faceShapeFilter.value = null
        _hasFlashDealFilter.value = false
        _ratingFilter.value = 0f
        _priceFilter.value = 1000f
        _atHomeService.value = false
        _lateNightService.value = false
        applyFilters()
    }
}