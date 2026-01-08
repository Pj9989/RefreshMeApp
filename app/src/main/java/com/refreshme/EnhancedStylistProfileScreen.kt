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
    val stylist by viewModel.stylist.collectAsState()

    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
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
            stylist?.let {
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
                        it.profileImageUrl?.let { imageUrl ->
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
                            Text(it.name, style = MaterialTheme.typography.headlineMedium)
                            it.specialty?.let { specialty ->
                                Text(specialty, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.primary)
                                Text("${it.rating}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    // Bio
                    it.bio?.let { bio ->
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
                    it.portfolioImages?.let { images ->
                        if (images.isNotEmpty()) {
                            Text("Portfolio", style = MaterialTheme.typography.headlineSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(images.values.toList()) { imageUrl ->
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
                    it.services?.let { services ->
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
                                        Text("${service.name} (${service.duration} mins)")
                                        Text("$${service.price}")
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}