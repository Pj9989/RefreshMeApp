package com.refreshme.aistylefinder

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.StyleProfile
import com.refreshme.data.Stylist
import com.refreshme.util.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AiStyleResultsViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val _recommendations = MutableStateFlow<List<AiStyleRecommendation>>(emptyList())
    val recommendations: StateFlow<List<AiStyleRecommendation>> = _recommendations.asStateFlow()

    private val _specialtyIds = MutableStateFlow<List<String>>(emptyList())
    val specialtyIds: StateFlow<List<String>> = _specialtyIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _matchingStylists = MutableStateFlow<List<Stylist>>(emptyList())
    val matchingStylists: StateFlow<List<Stylist>> = _matchingStylists.asStateFlow()

    fun processQuizResults(gender: String, vibe: String, frequency: String, finish: String, faceShape: String = "UNKNOWN") {
        val request = AiStyleRequest(gender, vibe, frequency, finish, faceShape)

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 1. Generate recommendations locally (Fast)
                val recommendations = RecommendationEngine.getRecommendations(request)
                _recommendations.value = recommendations

                // 2. Get matching specialties locally (Fast)
                val specialties = RecommendationEngine.getMatchingSpecialties(recommendations)
                _specialtyIds.value = specialties

                // 3. Save to profile with timeout to prevent hanging
                withTimeoutOrNull(5000) {
                    val profile = StyleProfile(
                        gender = gender,
                        vibe = vibe,
                        frequency = frequency,
                        finish = finish,
                        lastUpdated = System.currentTimeMillis()
                    )
                    UserManager.updateStyleProfile(profile)
                }

                // 4. Find matching stylists with timeout
                withTimeoutOrNull(8000) {
                    findMatchingStylists(specialties)
                }

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to process results"
                Log.e("AiStyleResultsVM", "Error processing quiz results", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun findMatchingStylists(specialties: List<String>) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) return

            // Query verified stylists
            val stylistsSnapshot = firestore.collection("stylists")
                .whereEqualTo("isVerified", true)
                .get()
                .await()

            val allStylists = stylistsSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            // Filter logic
            val matchingStylists = allStylists.filter { stylist ->
                val stylistSpecialty = stylist.specialty?.lowercase() ?: ""
                val stylistBio = stylist.bio?.lowercase() ?: ""
                
                specialties.any { recommendedTag ->
                    val keywords = getKeywordsForTag(recommendedTag)
                    keywords.any { keyword ->
                        stylistSpecialty.contains(keyword) || stylistBio.contains(keyword)
                    }
                }
            }
            
            val results = if (matchingStylists.isNotEmpty()) {
                matchingStylists
            } else {
                allStylists
            }

            _matchingStylists.value = results.sortedByDescending { it.rating }

        } catch (e: Exception) {
            Log.e("AiStyleResultsVM", "Error finding matching stylists", e)
        }
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
            else -> listOf(tag.replace("_", " ").lowercase())
        }
    }

    fun getFormattedRecommendations(): String {
        if (_recommendations.value.isEmpty()) {
            return "No recommendations available"
        }

        return _recommendations.value.joinToString("\n\n") { recommendation ->
            "Style: ${recommendation.styleName}\n\nReasoning: ${recommendation.reasoning}"
        }
    }

    fun hasMatchingStylists(): Boolean {
        return _matchingStylists.value.isNotEmpty()
    }

    fun getSpecialtyIdsArray(): Array<String> = _specialtyIds.value.toTypedArray()
}