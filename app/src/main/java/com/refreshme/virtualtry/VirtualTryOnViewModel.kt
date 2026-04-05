package com.refreshme.virtualtry

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * ViewModel for the Virtual Try-On feature.
 *
 * Extends AndroidViewModel to safely access the Application Context,
 * which is required for reading images from the Android Content Resolver
 * (converting a content:// Uri into a Base64-encoded JPEG string).
 *
 * Architecture:
 *  - UI state is exposed as StateFlow for Compose/Fragment observation
 *  - Base64 conversion runs on Dispatchers.IO to avoid blocking the main thread
 *  - Replicate API calls are delegated to ReplicateApiService
 */
class VirtualTryOnViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VirtualTryOnViewModel"

        // Hairstyle options presented to the user
        val HAIRSTYLE_OPTIONS = listOf(
            "Balayage Waves",
            "Box Braids",
            "Fade & Taper",
            "Afro",
            "Locs",
            "Pixie Cut",
            "Curtain Bangs",
            "Buzz Cut",
            "Natural Curls",
            "Straight & Sleek"
        )

        // Ethnicity options for more accurate prompt generation
        val ETHNICITY_OPTIONS = listOf(
            "Not Specified",
            "African American",
            "Asian",
            "Caucasian",
            "Hispanic / Latino",
            "Middle Eastern",
            "South Asian"
        )

        // Gender options for prompt accuracy
        val GENDER_OPTIONS = listOf("Not Specified", "Male", "Female", "Non-binary")
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    sealed class UiState {
        /** Initial state: no image selected, no result yet */
        object Idle : UiState()

        /** User has selected a selfie but hasn't generated yet */
        data class ImageSelected(val uri: Uri) : UiState()

        /** API call is in progress */
        object Loading : UiState()

        /** Generation succeeded; resultUrl is the Replicate output image URL */
        data class Success(val selfieUri: Uri, val resultUrl: String) : UiState()

        /** Something went wrong */
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── User Selections ───────────────────────────────────────────────────────

    private val _selectedHairstyle = MutableStateFlow(HAIRSTYLE_OPTIONS[0])
    val selectedHairstyle: StateFlow<String> = _selectedHairstyle.asStateFlow()

    private val _selectedEthnicity = MutableStateFlow(ETHNICITY_OPTIONS[0])
    val selectedEthnicity: StateFlow<String> = _selectedEthnicity.asStateFlow()

    private val _selectedGender = MutableStateFlow(GENDER_OPTIONS[0])
    val selectedGender: StateFlow<String> = _selectedGender.asStateFlow()

    // ── User Actions ──────────────────────────────────────────────────────────

    fun onHairstyleSelected(hairstyle: String) {
        _selectedHairstyle.value = hairstyle
    }

    fun onEthnicitySelected(ethnicity: String) {
        _selectedEthnicity.value = ethnicity
    }

    fun onGenderSelected(gender: String) {
        _selectedGender.value = gender
    }

    fun onImageSelected(uri: Uri) {
        _uiState.value = UiState.ImageSelected(uri)
    }

    fun resetToIdle() {
        _uiState.value = UiState.Idle
    }

    // ── Core Generation Logic ─────────────────────────────────────────────────

    /**
     * Entry point called when the user taps "Generate My Look".
     *
     * Flow:
     * 1. Validate that a selfie has been selected
     * 2. Convert the Uri → Base64 JPEG string (on IO thread)
     * 3. Build the AI prompt from user selections
     * 4. Call Replicate API and await result
     * 5. Update UiState with success or error
     *
     * @param apiKey The Replicate API token stored in BuildConfig or user prefs
     */
    fun generateTryOn(apiKey: String) {
        val currentState = _uiState.value
        val selfieUri = when (currentState) {
            is UiState.ImageSelected -> currentState.uri
            is UiState.Success -> currentState.selfieUri
            is UiState.Error -> {
                // If there was an error but we still have a URI, allow retry
                // by checking if we can recover it — for simplicity, show error
                _uiState.value = UiState.Error("Please select a photo first.")
                return
            }
            else -> {
                _uiState.value = UiState.Error("Please select a selfie first.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Step 1: Convert Uri to Base64 on the IO dispatcher
            val base64Image = uriToBase64(selfieUri)
            if (base64Image == null) {
                _uiState.value = UiState.Error(
                    "Could not read your photo. Please try selecting it again."
                )
                return@launch
            }

            // Step 2: Build the optimised AI prompt
            val prompt = buildPrompt()
            Log.d(TAG, "Generated prompt: $prompt")

            // Step 3: Call the Replicate API
            // ── 🚀 REAL AI INTEGRATION ACTIVE 🚀 ──────────────────────────────
            // ReplicateApiService handles:
            //   POST /v1/predictions  (creates job)
            //   GET  /v1/predictions/{id}  (polls until done)
            //
            // To switch models, update MODEL_VERSION in ReplicateApiService.kt
            // To use a different platform (Leonardo, fal.ai), swap this service.
            val result = ReplicateApiService.generateHairstyle(
                apiKey = apiKey,
                base64Image = base64Image,
                prompt = prompt
            )

            result.fold(
                onSuccess = { imageUrl ->
                    _uiState.value = UiState.Success(
                        selfieUri = selfieUri,
                        resultUrl = imageUrl
                    )
                },
                onFailure = { error ->
                    _uiState.value = UiState.Error(
                        error.message ?: "An unknown error occurred. Please try again."
                    )
                }
            )
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Converts a content:// Uri to a Base64-encoded JPEG data URI string.
     *
     * Uses the Application Context's ContentResolver to safely open the image
     * regardless of Android version or storage permission model.
     *
     * The image is:
     * - Decoded from the content stream
     * - Scaled down to max 1024×1024 to reduce API payload size
     * - Compressed to JPEG at 80% quality
     * - Encoded to Base64 and wrapped in a data URI prefix
     *
     * @return "data:image/jpeg;base64,<encoded>" or null on failure
     */
    private suspend fun uriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                return@withContext null
            }

            // Scale down to max 1024px on the longest side to stay within API limits
            val scaledBitmap = scaleBitmap(originalBitmap, maxSize = 1024)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()

            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"

        } catch (e: Exception) {
            Log.e(TAG, "uriToBase64 failed", e)
            null
        }
    }

    /**
     * Scales a Bitmap so neither dimension exceeds [maxSize], preserving aspect ratio.
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Builds a detailed, optimised AI prompt from the user's selections.
     *
     * Example output:
     * "A highly realistic professional portrait of an African American Female with a
     *  Box Braids hairstyle. Photorealistic, studio lighting, hyper-detailed, 8K."
     */
    private fun buildPrompt(): String {
        val hairstyle = _selectedHairstyle.value
        val ethnicity = _selectedEthnicity.value
        val gender = _selectedGender.value

        val subjectDescription = buildString {
            append("a person")
            if (ethnicity != "Not Specified") append(" who is $ethnicity")
            if (gender != "Not Specified") append(" and identifies as $gender")
        }

        return "A highly realistic professional portrait of $subjectDescription " +
                "with a $hairstyle hairstyle. " +
                "Photorealistic, studio lighting, hyper-detailed, 8K resolution, " +
                "sharp focus, natural skin texture, professional photography."
    }
}
