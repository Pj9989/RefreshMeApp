package com.refreshme

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Stylist
import com.refreshme.data.StylistRepository
import com.refreshme.util.AnalyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class FilterState {
    ALL,
    FAVORITES
}

@OptIn(FlowPreview::class)
class StylistListViewModel : ViewModel() {

    private val repository = StylistRepository()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _allStylists = MutableStateFlow<List<Stylist>>(emptyList())
    private val _favorites = MutableStateFlow<List<String>>(emptyList())
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()
    private val _filterState = MutableStateFlow(FilterState.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _specialtyFilter = MutableStateFlow<String?>(null)
    val specialtyFilter: StateFlow<String?> = _specialtyFilter.asStateFlow()
    private val _ratingFilter = MutableStateFlow<Float?>(null)
    val ratingFilter: StateFlow<Float?> = _ratingFilter.asStateFlow()
    private val _atHomeService = MutableStateFlow(false)
    val atHomeService: StateFlow<Boolean> = _atHomeService.asStateFlow()
    private val _userLocation = MutableStateFlow<Location?>(null)

    val searchQuery = _searchQuery.asStateFlow()
    val filterState = _filterState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val specialties: StateFlow<List<String>> = _allStylists.map { stylists ->
        stylists.mapNotNull { it.specialty }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class Filters(
        val stylists: List<Stylist>,
        val favorites: List<String>,
        val filter: FilterState,
        val query: String,
        val specialty: String?,
        val rating: Float?,
        val atHomeService: Boolean,
        val userLocation: Location?
    )

    private val filters = combine(
        _allStylists,
        _favorites,
        _filterState,
        _searchQuery,
        _specialtyFilter,
        _ratingFilter,
        _atHomeService,
        _userLocation
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        Filters(
            stylists = values[0] as List<Stylist>,
            favorites = values[1] as List<String>,
            filter = values[2] as FilterState,
            query = values[3] as String,
            specialty = values[4] as String?,
            rating = values[5] as Float?,
            atHomeService = values[6] as Boolean,
            userLocation = values[7] as Location?
        )
    }

    val activeFilterCount: StateFlow<Int> = combine(
        _specialtyFilter,
        _ratingFilter,
        _atHomeService
    ) { specialty, rating, atHome ->
        var count = 0
        if (specialty != null) count++
        if (rating != null && rating > 0f) count++
        if (atHome) count++
        count
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val stylists: StateFlow<List<Stylist>> =
        filters.map { filters ->
            val availableStylists = filters.stylists

            var filteredStylists: List<Stylist> = when (filters.filter) {
                FilterState.ALL -> availableStylists
                FilterState.FAVORITES -> availableStylists.filter { it.id in filters.favorites }
            }

            if (filters.query.isNotBlank()) {
                filteredStylists = filteredStylists.filter {
                    it.name.contains(filters.query, ignoreCase = true) ||
                            (it.address?.contains(filters.query, ignoreCase = true) == true)
                }
            }

            filters.specialty?.let { spec ->
                filteredStylists = filteredStylists.filter { it.specialty == spec }
            }

            filters.rating?.let { minRating ->
                filteredStylists = filteredStylists.filter { it.rating >= minRating }
            }

            if (filters.atHomeService) {
                filteredStylists = filteredStylists.filter { it.offersAtHomeService == true }
            }

            filteredStylists.sortedWith(
                compareBy<Stylist> { stylist ->
                    filters.userLocation?.let {
                        val stylistLocation = Location("").apply {
                            latitude = stylist.location?.latitude ?: 0.0
                            longitude = stylist.location?.longitude ?: 0.0
                        }
                        filters.userLocation.distanceTo(stylistLocation)
                    } ?: Float.MAX_VALUE
                }
                    .thenByDescending { it.rating }
            )
        }.flowOn(Dispatchers.Default)
            .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadStylists()
        loadFavorites()

        _searchQuery.debounce(500).onEach { query ->
            if (query.isNotBlank()) {
                AnalyticsHelper.logSearchStylists(query)
            }
        }.launchIn(viewModelScope)
    }

    fun refreshStylists() {
        loadStylists()
        loadFavorites()
    }

    fun setUserLocation(location: Location) {
        _userLocation.value = location
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: FilterState) {
        _filterState.value = filter
        AnalyticsHelper.logFilterApplied(filter.name)
    }

    fun setSpecialtyFilter(specialty: String?) {
        _specialtyFilter.value = specialty
        specialty?.let { AnalyticsHelper.logFilterApplied("specialty: $it") }
    }

    fun setRatingFilter(rating: Float?) {
        _ratingFilter.value = rating
        rating?.let { AnalyticsHelper.logFilterApplied("rating: $it") }
    }

    fun setAtHomeService(atHome: Boolean) {
        _atHomeService.value = atHome
        AnalyticsHelper.logFilterApplied("at_home_service: $atHome")
    }

    fun clearFilters() {
        _specialtyFilter.value = null
        _ratingFilter.value = null
        _atHomeService.value = false
    }

    fun toggleFavorite(stylist: Stylist) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val favoritesRef = firestore.collection("users").document(userId).collection("favorites")

            if (stylist.id in _favorites.value) {
                favoritesRef.document(stylist.id).delete().await()
                AnalyticsHelper.logFavoriteRemoved(stylist.id)
            } else {
                favoritesRef.document(stylist.id).set(mapOf("stylistId" to stylist.id)).await()
                AnalyticsHelper.logFavoriteAdded(stylist.id)
            }

            loadFavorites()
        }
    }

    private fun loadStylists() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _allStylists.value = repository.getStylists()
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
                Log.e("StylistListViewModel", "Error loading stylists", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val snapshot = firestore.collection("users").document(userId).collection("favorites").get().await()
                _favorites.value = snapshot.documents.mapNotNull { it.id }
            } catch (e: Exception) {
                Log.e("StylistListViewModel", "Error loading favorites", e)
            }
        }
    }
}