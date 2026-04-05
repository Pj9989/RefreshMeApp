package com.refreshme.aistylefinder

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStyleQuizScreen(
    onBack: () -> Unit,
    onScanFace: () -> Unit,
    onSubmit: () -> Unit,
    viewModel: AiStyleQuizViewModel = viewModel()
) {
    val selectedGender by viewModel.selectedGender.collectAsState()
    val selectedVibe by viewModel.selectedVibe.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()
    val selectedFinish by viewModel.selectedFinish.collectAsState()
    val faceShape by viewModel.faceShape.collectAsState()
    val canSubmit by viewModel.canSubmit.collectAsState()
    
    // UI local state to handle the loading transition
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Style Finder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    if (!isProcessing) {
                        isProcessing = true
                        onSubmit()
                    }
                },
                // Disable button immediately when processing
                enabled = canSubmit && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary, 
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Get Recommendations", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    "Let's find your perfect look.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Answer a few questions to get personalized hairstyle recommendations.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                QuizSection(
                    title = "Who are you finding a style for?",
                    options = listOf(
                        QuizOption("women", "Women"),
                        QuizOption("men", "Men")
                    ),
                    selectedId = selectedGender,
                    onSelect = { viewModel.selectGender(it) }
                )
            }

            item {
                QuizSection(
                    title = "What vibe do you want?",
                    options = listOf(
                        QuizOption("clean_classic", "Clean & Classic"),
                        QuizOption("bold_trendy", "Bold & Trendy"),
                        QuizOption("low_maintenance", "Low Maintenance")
                    ),
                    selectedId = selectedVibe,
                    onSelect = { viewModel.selectVibe(it) }
                )
            }

            item {
                QuizSection(
                    title = "How often do you want to get a cut?",
                    options = listOf(
                        QuizOption("weekly", "Weekly"),
                        QuizOption("biweekly", "Every 2 weeks"),
                        QuizOption("monthly", "Monthly")
                    ),
                    selectedId = selectedFrequency,
                    onSelect = { viewModel.selectFrequency(it) }
                )
            }

            item {
                QuizSection(
                    title = "What finish do you like?",
                    options = listOf(
                        QuizOption("natural", "Natural look"),
                        QuizOption("sharp", "Sharp line-up"),
                        QuizOption("new", "Try something new")
                    ),
                    selectedId = selectedFinish,
                    onSelect = { viewModel.selectFinish(it) }
                )
            }

            item {
                FaceScanCard(
                    faceShape = faceShape,
                    onScan = {
                        if (!isProcessing) onScanFace()
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

data class QuizOption(val id: String, val label: String)

@Composable
fun QuizSection(
    title: String,
    options: List<QuizOption>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectableOption(
                    label = option.label,
                    isSelected = option.id == selectedId,
                    onClick = { onSelect(option.id) }
                )
            }
        }
    }
}

@Composable
fun SelectableOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FaceScanCard(faceShape: String, onScan: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onScan() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Scan Face for Precision", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (faceShape != "UNKNOWN") {
                    Text(
                        "Detected: $faceShape", 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        "Optional - Get better matches", 
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
