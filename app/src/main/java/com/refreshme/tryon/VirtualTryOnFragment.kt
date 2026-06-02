package com.refreshme.tryon

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.ui.theme.RefreshMeTheme
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class VirtualTryOnFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    VirtualTryOnScreen(
                        onNavigateBack = { findNavController().popBackStack() }
                    )
                }
            }
        }
    }
}

data class VirtualTryOnState(
    val selectedImageUri: Uri? = null,
    val selectedGender: String = "Female",
    val selectedRace: String = "Caucasian",
    val selectedHairstyle: String = "Balayage Waves",
    val isGenerating: Boolean = false,
    val generatedImageUrl: String? = null
)

class VirtualTryOnViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val DEFAULT_MODEL_VERSION = "39ed52f2a78e934b3ba6e2a89f5b1c712de7dfea535525255b1aa35c5565e08b"
        const val NEGATIVE_PROMPT = "different face, altered facial features, face swap, changed identity, " +
            "changed expression, professional studio portrait, painting, illustration, fake, 3d render, " +
            "morphed, changed background, changed clothing, blur, oversaturated, beauty filter, " +
            "plastic skin, watermark, text"
    }

    private val _uiState = MutableStateFlow(VirtualTryOnState())
    val uiState: StateFlow<VirtualTryOnState> = _uiState.asStateFlow()
    private val functions = FirebaseFunctions.getInstance()

    fun updateSelectedImage(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedImageUri = uri, generatedImageUrl = null)
    }

    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(selectedGender = gender)
    }

    fun updateRace(race: String) {
        _uiState.value = _uiState.value.copy(selectedRace = race)
    }

    fun updateHairstyle(style: String) {
        _uiState.value = _uiState.value.copy(selectedHairstyle = style)
    }

    fun generateTryOn() {
        val currentState = _uiState.value
        val imageUri = currentState.selectedImageUri ?: return

        _uiState.value = currentState.copy(isGenerating = true, generatedImageUrl = null)

        viewModelScope.launch {
            try {
                // 1. Convert the selected image to Base64
                val base64Image = withContext(Dispatchers.IO) {
                    uriToBase64(imageUri)
                }
                
                // 2. Build the AI Prompt for FLUX.1 Kontext (prompt-driven local edit).
                val prompt = "Change the hairstyle of this ${currentState.selectedRace} ${currentState.selectedGender} to a realistic ${currentState.selectedHairstyle}. " +
                    "Keep the exact same face, identity, skin tone, eyes, expression, head pose, lighting, background, and clothing unchanged. " +
                    "Photorealistic. Natural hair texture."

                // 3. Call the Cloud Function (which proxies to flux-kontext-pro).
                val resultUrl = withContext(Dispatchers.IO) {
                    callReplicateApi(base64Image, prompt)
                }

                if (resultUrl != null) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedImageUrl = resultUrl
                    )
                } else {
                    Log.e("VirtualTryOn", "Result URL was null")
                    _uiState.value = _uiState.value.copy(isGenerating = false)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    private suspend fun callReplicateApi(base64Image: String?, prompt: String): String? {
        if (base64Image == null) return null
        val result = functions
            .getHttpsCallable("runVirtualTryOn")
            .call(
                mapOf(
                    "base64Image" to base64Image,
                    "prompt" to prompt,
                    "modelVersion" to DEFAULT_MODEL_VERSION,
                    "negativePrompt" to NEGATIVE_PROMPT
                )
            )
            .await()
        val data = result.data as? Map<*, *>
        return (data?.get("outputUrl") as? String) ?: (data?.get("url") as? String)
    }

    /**
     * Helper to convert an Android Content Uri into a compressed Base64 string 
     * which is required by Image-to-Image AI APIs.
     */
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val outputStream = ByteArrayOutputStream()
            val maxImageSize = 1536f
            val ratio = Math.min(
                maxImageSize / bitmap.width,
                maxImageSize / bitmap.height
            )
            val scaledBitmap = if (ratio < 1.0f) {
                bitmap.scale((bitmap.width * ratio).roundToInt(), (bitmap.height * ratio).roundToInt(), true)
            } else {
                bitmap
            }
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

val GENDERS = listOf("Female", "Male", "Non-Binary")
val RACES = listOf("Caucasian", "African American", "Asian", "Hispanic", "Middle Eastern", "Mixed")
val HAIRSTYLES = listOf("Balayage Waves", "Pixie Cut", "Bob Cut", "Curtain Bangs", "Buzz Cut", "Fade", "Dreadlocks", "Braids", "Layered Shag")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualTryOnScreen(
    viewModel: VirtualTryOnViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.updateSelectedImage(it) } }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Virtual Try-On", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                "See what you look like with a new style!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Image Picker Area
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (uiState.selectedImageUri == null && uiState.generatedImageUrl == null) {
                            "Upload selfie"
                        } else {
                            "Upload or replace selfie"
                        }
                        stateDescription = when {
                            uiState.isGenerating -> "Generating try-on"
                            uiState.generatedImageUrl != null -> "Generated image shown"
                            uiState.selectedImageUri != null -> "Selfie selected"
                            else -> "No selfie selected"
                        }
                    }
                    .clickable { 
                        if (!uiState.isGenerating) {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.generatedImageUrl != null) {
                    AsyncImage(
                        model = uiState.generatedImageUrl,
                        contentDescription = "Generated Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (uiState.selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(uiState.selectedImageUri),
                        contentDescription = "Selected Selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Upload Selfie", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (uiState.isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text("Styling hair...", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (uiState.generatedImageUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("✨ AI Result ✨", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.updateSelectedImage(uiState.selectedImageUri) }) {
                    Text("Clear Result")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Configuration Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SelectionSection("Gender", GENDERS, uiState.selectedGender) { viewModel.updateGender(it) }
                SelectionSection("Race / Ethnicity", RACES, uiState.selectedRace) { viewModel.updateRace(it) }
                SelectionSection("Desired Hairstyle", HAIRSTYLES, uiState.selectedHairstyle) { viewModel.updateHairstyle(it) }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateTryOn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = uiState.selectedImageUri != null && !uiState.isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.isGenerating) "Generating Magic..." else "Generate Try-On", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (uiState.selectedImageUri == null) {
                    Text(
                        "Please upload a selfie to continue.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun SelectionSection(title: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selectedOption
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onOptionSelected(option) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = option,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
