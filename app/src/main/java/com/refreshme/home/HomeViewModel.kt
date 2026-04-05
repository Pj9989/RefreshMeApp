package com.refreshme.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.refreshme.util.UserManager
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _stylists = MutableLiveData<List<Stylist>>()
    val stylists: LiveData<List<Stylist>> = _stylists

    private val _recommendedStylists = MutableLiveData<List<Stylist>>()
    val recommendedStylists: LiveData<List<Stylist>> = _recommendedStylists

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _styleVibe = MutableLiveData<String?>()
    val styleVibe: LiveData<String?> = _styleVibe

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentUserLocation: Location? = null
    private var allLiveStylists: List<Stylist> = emptyList()
    private var userProfile: StyleProfile? = null
    
    private var userListener: ListenerRegistration? = null

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
                    _userName.postValue("What's good, ${user.name} 👋")
                    userProfile = user.styleProfile
                    
                    user.styleProfile?.let { profile ->
                        val formattedVibe = when(profile.vibe) {
                            "clean_classic" -> "Classic & Clean"
                            "bold_trendy" -> "Bold & Trendy"
                            "low_maintenance" -> "Low Maintenance"
                            else -> null
                        }
                        _styleVibe.postValue(formattedVibe)
                        fetchRecommendedStylists(profile)
                    }
                }
            }
    }
    
    fun updateUserLocation(location: Location) {
        currentUserLocation = location
        recalculateStylistDistances(_stylists, allLiveStylists)
        _recommendedStylists.value?.let { recalculateStylistDistances(_recommendedStylists, it) }
    }

    private fun fetchLiveStylists() {
        if (auth.currentUser == null) {
            _stylists.value = emptyList()
            _isLoading.postValue(false)
            return
        }

        db.collection("stylists")
            .whereEqualTo("availableNow", true)
            .addSnapshotListener { snapshots, e ->
                _isLoading.postValue(false)
                if (e != null) {
                    Log.w("HomeViewModel", "Listen failed.", e)
                    _stylists.value = emptyList()
                    return@addSnapshotListener
                }

                allLiveStylists = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
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
                    db.collection("stylists")
                        .whereIn("specialty", specialties)
                        .limit(10)
                        .get()
                        .addOnSuccessListener { snapshots ->
                            val stylists = snapshots.documents.mapNotNull { doc ->
                                doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                            }
                            stylists.forEach { stylist ->
                                stylist.matchScore = 95
                                stylist.matchExplanation = RecommendationEngine.getMatchExplanation(stylist.specialty, request)
                            }
                            recalculateStylistDistances(_recommendedStylists, stylists)
                        }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching recommended stylists", e)
            }
        }
    }
    
    private fun recalculateStylistDistances(targetLiveData: MutableLiveData<List<Stylist>>, list: List<Stylist>) {
        val updatedList = list.toList()
        currentUserLocation?.let { location ->
            updatedList.forEach { stylist ->
                stylist.location?.let {
                    val stylistLocation = Location("").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                    val distanceInMeters = location.distanceTo(stylistLocation)
                    stylist.distance = (distanceInMeters / 1609.34).roundTo(1)
                }
            }
            targetLiveData.value = updatedList.sortedBy { it.distance }
        } ?: run {
            targetLiveData.value = updatedList
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
    }
}