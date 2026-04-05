package com.refreshme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedStylistProfileScreen(
    stylistId: String,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: StylistProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stylist = uiState.stylist
    
    LaunchedEffect(stylistId) {
        // Only fetch if not already loading or loaded
        if (stylistId.isNotEmpty() && !uiState.isLoading && stylist == null) {
            viewModel.fetchStylist(stylistId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stylist?.name ?: "Stylist Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                stylist != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            stylist.profileImageUrl?.let { imageUrl ->
                                Image(
                                    painter = rememberImagePainter(data = imageUrl),
                                    contentDescription = "Stylist Profile Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stylist.name, style = MaterialTheme.typography.headlineMedium)
                                stylist.specialty?.let { specialty ->
                                    Text(specialty, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.primary)
                                    Text("${stylist.rating}", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        // Bio
                        stylist.bio?.let { bio ->
                            if (bio.isNotBlank()) {
                                Text("Bio", style = MaterialTheme.typography.headlineSmall)
                                Text(bio, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onBookClick(stylistId) }) {
                                Text("Book")
                            }
                            Button(onClick = { onChatClick(stylistId) }) {
                                Text("Chat")
                            }
                        }

                        // Portfolio
                        stylist.portfolioImages?.let { images ->
                            if (images.isNotEmpty()) {
                                Text("Portfolio", style = MaterialTheme.typography.headlineSmall)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(images) { imageUrl ->
                                        Image(
                                            painter = rememberImagePainter(data = imageUrl),
                                            contentDescription = "Portfolio Image",
                                            modifier = Modifier.size(150.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Services
                        stylist.services?.let { services ->
                            if (services.isNotEmpty()) {
                                Text("Services", style = MaterialTheme.typography.headlineSmall)
                                services.forEach { service ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // FIX: Use durationMinutes instead of duration
                                            Text("${service.name} (${service.durationMinutes} mins)")
                                            Text("$${service.price}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Stylist is null and not loading (e.g., fetch was attempted and failed/returned null)
                    Text(text = "Stylist profile not found.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}