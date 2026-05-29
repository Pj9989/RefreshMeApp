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
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.refreshme.data.BeforeAfter
import com.refreshme.ui.components.BeforeAfterImageSlider
import com.refreshme.ui.components.rememberFirebaseImageModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnhancedStylistProfileScreen(
    stylistId: String,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onServiceClick: (com.refreshme.data.Service) -> Unit = {},
    viewModel: com.refreshme.details.StylistDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Map StylistDetailViewModel's state to local variables
    val isLoading = uiState is com.refreshme.details.StylistUiState.Loading
    val error = (uiState as? com.refreshme.details.StylistUiState.Error)?.message
    val stylist = (uiState as? com.refreshme.details.StylistUiState.Success)?.stylist

    LaunchedEffect(stylistId) {
        if (stylistId.isNotEmpty()) {
            viewModel.getStylist(stylistId)
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
        },
        bottomBar = {
            if (stylist != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onChatClick(stylistId) },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Chat")
                        }
                        Button(
                            onClick = { onBookClick(stylistId) },
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Book Now")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(text = "Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
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
                                    painter = rememberAsyncImagePainter(model = rememberFirebaseImageModel(imageUrl)),
                                    contentDescription = "Stylist Profile Image",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stylist.name, style = MaterialTheme.typography.headlineMedium)
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (stylist.isOnline == true) Color(0xFF4CAF50) else Color(0xFF9E9E9E), CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (stylist.isOnline == true) "Accepting Appointments" else "Currently Offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (stylist.isOnline == true) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                stylist.specialty?.let { specialty ->
                                    Text(specialty, style = MaterialTheme.typography.bodyLarge)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(String.format(java.util.Locale.US, "%.1f", stylist.rating), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                }
                                
                                val offersMobile = (stylist.offersAtHomeService == true || 
                                                 stylist.serviceType == com.refreshme.data.ServiceType.AT_HOME || 
                                                 stylist.serviceType == com.refreshme.data.ServiceType.ALL_HOURS)
                                                 
                                if (offersMobile) {
                                    Spacer(Modifier.height(4.dp))
                                    MobileServiceStatus(fee = stylist.effectiveAtHomeServiceFee, range = stylist.maxTravelRangeKm ?: 15)
                                }
                            }
                        }

                        // Bio
                        stylist.bio?.let { bio ->
                            if (bio.isNotBlank()) {
                                Text("Bio", style = MaterialTheme.typography.headlineSmall)
                                Text(bio, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Serves
                        val genders = stylist.servesGender
                        if (!genders.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                genders.forEach { g ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(g, style = MaterialTheme.typography.labelMedium) },
                                        icon = {
                                            Icon(
                                                Icons.Default.People,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Portfolio Reels (Videos)
                        stylist.portfolioVideos?.let { videos ->
                            if (videos.isNotEmpty()) {
                                Text("Reels", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(videos) { videoUrl ->
                                        VideoReelCard(videoUrl, context)
                                    }
                                }
                            }
                        }

                        // Style Vault (Transformations)
                        stylist.beforeAfterImages?.let { beforeAfterList ->
                            if (beforeAfterList.isNotEmpty()) {
                                Text("Style Vault", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(beforeAfterList) { item ->
                                        BeforeAfterCard(item)
                                    }
                                }
                            }
                        }

                        // Portfolio
                        stylist.portfolioImages?.let { images ->
                            if (images.isNotEmpty()) {
                                Text("Portfolio", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(images) { imageUrl ->
                                        Image(
                                            painter = rememberAsyncImagePainter(model = rememberFirebaseImageModel(imageUrl)),
                                            contentDescription = "Portfolio Image",
                                            modifier = Modifier.size(150.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        // Services Section (split by Packages & Bundles and A La Carte)
                        stylist.services?.let { services ->
                            if (services.isNotEmpty()) {
                                val bundles = services.filter { it.isBundle }
                                val singleServices = services.filter { !it.isBundle }

                                if (bundles.isNotEmpty()) {
                                    Text("Packages & Bundles", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                                    bundles.forEach { bundle ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onServiceClick(bundle) },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(bundle.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                    }
                                                    if (bundle.description.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(bundle.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("${bundle.durationMinutes} mins", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        "$${String.format(java.util.Locale.US, "%.2f", bundle.price)}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    ) {
                                                        Text("BOOK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (singleServices.isNotEmpty()) {
                                    Text("A La Carte Services", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Start))
                                    singleServices.forEach { service ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onServiceClick(service) },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("${service.durationMinutes} mins", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        "$${String.format(java.util.Locale.US, "%.2f", service.price)}",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    ) {
                                                        Text("BOOK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
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

@Composable
fun MobileServiceStatus(fee: Double, range: Int) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "Offers House Calls",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            val rangeMiles = range.toDouble() * 0.621371
            Text(
                "Travels up to ${String.format(java.util.Locale.US, "%.0f", rangeMiles)} mi ($${String.format(java.util.Locale.US, "%.0f", fee)} fee)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VideoReelCard(url: String, context: android.content.Context) {
    Card(
        modifier = Modifier
            .size(160.dp, 280.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Placeholder/Thumbnail logic
            Icon(
                Icons.Default.PlayCircle, 
                contentDescription = null, 
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp).align(Alignment.Center)
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) {
                Text(
                    "REEL", 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BeforeAfterCard(item: BeforeAfter) {
    var showNotes by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .size(300.dp, 400.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            BeforeAfterImageSlider(
                beforeImageUrl = item.beforeImageUrl,
                afterImageUrl = item.afterImageUrl,
                modifier = Modifier.fillMaxSize()
            )

            if (item.technicalNotes.isNotEmpty()) {
                IconButton(
                    onClick = { showNotes = !showNotes },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Notes", tint = Color.White)
                }
            }

            if (showNotes) {
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Technical Notes", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(item.technicalNotes, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                    .padding(16.dp)
            ) {
                Column {
                    Text(item.description, color = Color.White, fontWeight = FontWeight.Bold)
                    if (item.tags.isNotEmpty()) {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            item.tags.take(3).forEach { tag ->
                                Text("#$tag ", color = Color.Cyan, fontSize = 10.sp)
                            }
                        }
                    }
                    Text("Drag slider to compare", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
    }
}
