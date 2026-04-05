package com.refreshme.aistylefinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refreshme.util.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiStyleQuizViewModel : ViewModel() {

    private val _selectedGender = MutableStateFlow<String?>(null)
    val selectedGender: StateFlow<String?> = _selectedGender.asStateFlow()

    private val _selectedVibe = MutableStateFlow<String?>(null)
    val selectedVibe: StateFlow<String?> = _selectedVibe.asStateFlow()

    private val _selectedFrequency = MutableStateFlow<String?>(null)
    val selectedFrequency: StateFlow<String?> = _selectedFrequency.asStateFlow()

    private val _selectedFinish = MutableStateFlow<String?>(null)
    val selectedFinish: StateFlow<String?> = _selectedFinish.asStateFlow()

    private val _faceShape = MutableStateFlow("UNKNOWN")
    val faceShape: StateFlow<String> = _faceShape.asStateFlow()

    private val _canSubmit = MutableStateFlow(false)
    val canSubmit: StateFlow<Boolean> = _canSubmit.asStateFlow()

    init {
        loadExistingProfile()
    }

    private fun loadExistingProfile() {
        viewModelScope.launch {
            val user = UserManager.getCurrentUser()
            user?.styleProfile?.let { profile ->
                _selectedGender.value = profile.gender
                _selectedVibe.value = profile.vibe
                _selectedFrequency.value = profile.frequency
                _selectedFinish.value = profile.finish
                // Note: We might want to store faceShape in StyleProfile too later
                updateCanSubmit()
            }
        }
    }

    fun selectGender(gender: String) {
        _selectedGender.value = gender
        updateCanSubmit()
    }

    fun selectVibe(vibe: String) {
        _selectedVibe.value = vibe
        updateCanSubmit()
    }

    fun selectFrequency(frequency: String) {
        _selectedFrequency.value = frequency
        updateCanSubmit()
    }

    fun selectFinish(finish: String) {
        _selectedFinish.value = finish
        updateCanSubmit()
    }

    fun setFaceShape(shape: String) {
        _faceShape.value = shape
    }

    private fun updateCanSubmit() {
        _canSubmit.value = _selectedGender.value != null &&
                _selectedVibe.value != null &&
                _selectedFrequency.value != null &&
                _selectedFinish.value != null
    }

    fun getQuizRequest(): AiStyleRequest? {
        val gender = _selectedGender.value ?: return null
        val vibe = _selectedVibe.value ?: return null
        val frequency = _selectedFrequency.value ?: return null
        val finish = _selectedFinish.value ?: return null

        return AiStyleRequest(gender, vibe, frequency, finish, _faceShape.value)
    }

    fun reset() {
        _selectedGender.value = null
        _selectedVibe.value = null
        _selectedFrequency.value = null
        _selectedFinish.value = null
        _faceShape.value = "UNKNOWN"
        _canSubmit.value = false
    }
}