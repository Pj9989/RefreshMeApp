package com.refreshme.tryon

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.refreshme.ui.theme.RefreshMeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

// --- VIEW MODEL ---
class VirtualTryOnViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VirtualTryOnState())
    val uiState: StateFlow<VirtualTryOnState> = _uiState.asStateFlow()

    fun updateSelectedImage(uri: Uri) {
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
        if (currentState.selectedImageUri == null) return

        _uiState.value = currentState.copy(isGenerating = true, generatedImageUrl = null)

        viewModelScope.launch {
            // TODO: Here you would upload the selectedImageUri to Firebase Storage or send it directly 
            // as Base64 to an AI API (like Replicate, fal.ai, or Leonardo.ai) along with the prompt.
            
            // val prompt = "Realistic portrait of a ${currentState.selectedRace} ${currentState.selectedGender} with a ${currentState.selectedHairstyle} haircut. High quality, salon lighting."
            // val resultUrl = AiApi.generateImage(prompt, currentState.selectedImageUri)

            // Simulating a network request for AI image generation (3 seconds)
            delay(3000)

            // Mocking the result URL (this should come from the AI API response)
            val mockGeneratedUrl = "https://images.unsplash.com/photo-1595476108010-b4d1f10d5e42?q=80&w=600&auto=format&fit=crop"

            _uiState.value = _uiState.value.copy(
                isGenerating = false,
                generatedImageUrl = mockGeneratedUrl
            )
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

val GENDERS = listOf("Female", "Male", "Non-Binary")
val RACES = listOf("Caucasian", "African American", "Asian", "Hispanic", "Middle Eastern", "Mixed")
val HAIRSTYLES = listOf("Balayage Waves", "Pixie Cut", "Bob Cut", "Curtain Bangs", "Buzz Cut", "Fade", "Dreadlocks", "Braids")

// --- UI SCREEN ---
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            if (uiState.generatedImageUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("✨ AI Result ✨", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.updateSelectedImage(uiState.selectedImageUri!!) }) {
                    Text("Re-upload / Reset")
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
                SelectionSection("Race/Ethnicity", RACES, uiState.selectedRace) { viewModel.updateRace(it) }
                SelectionSection("Desired Hairstyle", HAIRSTYLES, uiState.selectedHairstyle) { viewModel.updateHairstyle(it) }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.generateTryOn() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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
                        "Please upload a selfie first.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
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