package com.refreshme.details

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.refreshme.RatingDialog
import com.refreshme.data.BeforeAfter
import com.refreshme.data.Review
import com.refreshme.data.Service
import com.refreshme.data.ServiceType
import com.refreshme.data.Stylist
import com.refreshme.data.Booking
import com.refreshme.ui.components.BeforeAfterImageSlider
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistDetailScreen(
    stylistId: String,
    viewModel: StylistDetailViewModel = viewModel(),
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onServiceClick: (Service) -> Unit
) {
    LaunchedEffect(stylistId) {
        viewModel.getStylist(stylistId)
    }
    val uiState by viewModel.uiState.collectAsState()
    val photosState by viewModel.photos.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val eligibleBooking by viewModel.eligibleBooking.collectAsState()
    val nextSlot by viewModel.nextAvailableSlot.collectAsState()
    val reportSuccess by viewModel.reportSuccess.collectAsState()
    val context = LocalContext.current
    
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAllReviews by remember { mutableStateOf(false) }

    LaunchedEffect(reportSuccess) {
        if (reportSuccess == true) {
            Toast.makeText(context, "Report submitted successfully. We will review it shortly.", Toast.LENGTH_LONG).show()
            viewModel.resetReportStatus()
            showReportDialog = false
        } else if (reportSuccess == false) {
            Toast.makeText(context, "Failed to submit report. Please try again.", Toast.LENGTH_LONG).show()
            viewModel.resetReportStatus()
        }
    }

    if (showRatingDialog) {
        val bookingForDialog = eligibleBooking ?: Booking(stylistId = stylistId, stylistName = (uiState as? StylistUiState.Success)?.stylist?.name ?: "Stylist")
        
        RatingDialog(
            booking = bookingForDialog,
            onDismiss = { showRatingDialog = false },
            onSubmit = { rating, comment ->
                viewModel.submitReview(stylistId, rating, comment)
                showRatingDialog = false
            }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.reportStylist(stylistId, reason, details)
            }
        )
    }

    Scaffold(
        bottomBar = {
            val stylist = when (uiState) {
                is StylistUiState.Success -> (uiState as StylistUiState.Success).stylist
                else -> null
            }

            if (stylist != null) {
                Surface(
                    modifier = Modifier.shadow(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (nextSlot != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bolt, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp), 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Next Available: $nextSlot", 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onChatClick(stylistId) },
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Chat")
                            }
                            Button(
                                onClick = { onBookClick(stylistId) },
                                enabled = stylist.isOnline == true,
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(if (stylist.isOnline == true) Icons.Default.CalendarToday else Icons.Default.Block, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (stylist.isOnline == true) "Book Now" else "Offline",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValue ->
        when (uiState) {
            is StylistUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(paddingValue), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            }
            is StylistUiState.Error -> {
                val error = (uiState as StylistUiState.Error).message
                Box(Modifier.fillMaxSize().padding(paddingValue), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(
                            Icons.Default.ErrorOutline, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp), 
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Oops! Something went wrong", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        Button(
                            onClick = { viewModel.getStylist(stylistId) },
                            modifier = Modifier.padding(top = 32.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
            is StylistUiState.Success -> {
                val s = (uiState as StylistUiState.Success).stylist
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValue)
                ) {
                    // Immersive Header
                    item {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)) {
                            if (s.displayImageUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(100.dp), 
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(s.displayImageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Check out this stylist on RefreshMe")
                                                putExtra(Intent.EXTRA_TEXT, "Check out ${s.name} on RefreshMe! They specialize in ${s.specialty ?: "amazing styles"}.\n\nhttps://refreshme-74f79.web.app/stylist/${s.id}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Stylist Profile"))
                                        },
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                                    }
                                    
                                    IconButton(
                                        onClick = { showRatingDialog = true },
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = "Rate", tint = Color(0xFFFFC107))
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(stylistId) },
                                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    ) {
                                        Icon(
                                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                                            contentDescription = "Favorite", 
                                            tint = if (isFavorite) Color.Red else Color.White
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                if (s.isVerifiedStylist) {
                                    AnimatedVerifiedBadge()
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    s.name, 
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    s.specialty.orEmpty().ifBlank { "Professional Stylist" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    // AI Vibe Check Section
                    if (aiSummary != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("AI Vibe Check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        aiSummary!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        lineHeight = 20.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Summarized from ${reviews.size} reviews",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Security & Trust Banner
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = "Safety", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Trust & Safety Guarantee", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                                    Text("Secure payments. ID Verified. 24/7 Support.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }

                    // Portfolio Reels (Videos)
                    if (s.portfolioVideos?.isNotEmpty() == true) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Red)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Portfolio Reels", 
                                        style = MaterialTheme.typography.titleLarge, 
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(s.portfolioVideos!!) { videoUrl ->
                                        VideoReelCard(videoUrl)
                                    }
                                }
                            }
                        }
                    }

                    // Social Proof & Highlights
                    item {
                        Column(modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Group, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary, 
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Booked 12 times this week", 
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (s.matchScore > 0) {
                                    MatchScoreBadge(s.matchScore)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem(
                                    label = "Rating", 
                                    value = String.format(Locale.US, "%.1f", s.rating), 
                                    icon = Icons.Default.Star, 
                                    iconColor = Color(0xFFFFC107),
                                    onClick = { showRatingDialog = true }
                                )
                                VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                StatItem(label = "Experience", value = "${s.yearsOfExperience ?: 0}y+", icon = Icons.Outlined.History)
                                VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                StatItem(label = "Reviews", value = "${reviews.size}", icon = Icons.AutoMirrored.Outlined.Chat)
                            }

                            Spacer(Modifier.height(20.dp))

                            // Flash Deal Badge if active
                            if (s.hasActiveFlashDeal) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(s.currentFlashDeal?.title ?: "Flash Deal", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            Text("${s.currentFlashDeal?.discountPercentage}% OFF", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            OnlineOfflineStatus(isOnline = s.isOnline ?: false)
                            
                            val offersMobile = s.offersAtHomeService == true || 
                                             s.serviceType == ServiceType.AT_HOME || 
                                             s.serviceType == ServiceType.ALL_HOURS
                            
                            if (offersMobile) {
                                Spacer(Modifier.height(12.dp))
                                MobileServiceStatus(fee = s.atHomeServiceFee ?: 0.0, range = s.maxTravelRangeKm ?: 15)
                            }
                            
                            if (s.offersEventBooking == true) {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF9575CD), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Available for Groups & Events",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF9575CD),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Style Vault (Transformations)
                    if (s.beforeAfterImages?.isNotEmpty() == true) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFFFFD700))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Style Vault", 
                                        style = MaterialTheme.typography.titleLarge, 
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(s.beforeAfterImages!!) { item ->
                                        BeforeAfterCard(item)
                                    }
                                }
                            }
                        }
                    }

                    // Portfolio Gallery
                    val portfolioUrls = (s.portfolioImages.orEmpty() + if (photosState is PhotosUiState.Success) (photosState as PhotosUiState.Success).photoUrls else emptyList()).distinct()
                    if (portfolioUrls.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Text(
                                    "Work Portfolio", 
                                    style = MaterialTheme.typography.titleLarge, 
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(portfolioUrls) { img ->
                                        TransformationCard(img)
                                    }
                                }
                            }
                        }
                    }

                    // Social Media Links Section
                    if (s.socialLinks != null && (!s.socialLinks.instagram.isNullOrBlank() || !s.socialLinks.tiktok.isNullOrBlank() || !s.socialLinks.website.isNullOrBlank())) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!s.socialLinks.instagram.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val url = s.socialLinks.instagram.let { if (it.startsWith("http")) it else "https://instagram.com/${it.removePrefix("@")}" }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Text("Instagram", fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (!s.socialLinks.tiktok.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val url = s.socialLinks.tiktok.let { if (it.startsWith("http")) it else "https://tiktok.com/${it.takeIf { t -> t.startsWith("@") } ?: "@$it"}" }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Text("TikTok", fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (!s.socialLinks.website.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val url = s.socialLinks.website.let { if (!it.startsWith("http")) "https://$it" else it }
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Website", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // About
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Bio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s.bio ?: "Professional stylist dedicated to excellence.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 24.sp
                            )
                            
                            if (s.vibes?.isNotEmpty() == true) {
                                Spacer(Modifier.height(12.dp))
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    s.vibes!!.forEach { vibe ->
                                        Surface(
                                            modifier = Modifier.padding(end = 8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                vibe, 
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Services Section
                    val allServices = s.services.orEmpty()
                    val bundles = allServices.filter { it.isBundle }
                    val singleServices = allServices.filter { !it.isBundle }

                    if (bundles.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                                Text("Packages & Bundles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        items(bundles) { bundle ->
                            ServiceItem(service = bundle, onClick = { onServiceClick(bundle) })
                        }
                    }

                    if (singleServices.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                                Text("A La Carte Services", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        items(singleServices) { service ->
                            ServiceItem(service = service, onClick = { onServiceClick(service) })
                        }
                    }

                    // Reviews Section
                    if (reviews.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Client Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        
                        val displayedReviews = if (showAllReviews) reviews else reviews.take(3)
                        
                        items(displayedReviews) { review ->
                            ReviewItem(review)
                        }
                        
                        if (reviews.size > 3) {
                            item {
                                TextButton(
                                    onClick = { showAllReviews = !showAllReviews },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                                ) {
                                    Text(if (showAllReviews) "Show Less" else "See All ${reviews.size} Reviews")
                                }
                            }
                        }
                    }

                    // Map Section
                    if (s.location != null && s.location.latitude != 0.0) {
                        item {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text(s.address ?: "In-shop appointments", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                
                                val latLng = LatLng(s.location.latitude, s.location.longitude)
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = cameraPositionState,
                                        uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, mapToolbarEnabled = false),
                                        onMapClick = {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${s.location.latitude},${s.location.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.location.latitude},${s.location.longitude}"))
                                                context.startActivity(browserIntent)
                                            }
                                        }
                                    ) {
                                        Marker(state = MarkerState(position = latLng), title = s.name)
                                    }
                                    
                                    // Get Directions Overlay
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shadowElevation = 4.dp,
                                        onClick = {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${s.location.latitude},${s.location.longitude}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.location.latitude},${s.location.longitude}"))
                                                context.startActivity(browserIntent)
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Directions, contentDescription = "Get Directions", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Get Directions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Trust and Safety Footer Actions
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            TextButton(onClick = { showReportDialog = true }) {
                                Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text("Report this Stylist", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                            Text("RefreshMe reviews reports to ensure community safety.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun VideoReelCard(url: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .size(160.dp, 280.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(16.dp),
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
                shape = RoundedCornerShape(8.dp)
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
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    
    val reasons = listOf(
        "Inappropriate Behavior",
        "Fake Profile / Scammer",
        "Did not show up to appointment",
        "Unsafe Environment",
        "Payment Issue outside of app"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Stylist", color = MaterialTheme.colorScheme.error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your safety is our top priority. Please let us know why you are reporting this stylist. This report will be kept confidential.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, fontSize = 14.sp)
                    }
                }
                
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Additional Details (Optional)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                enabled = selectedReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatItem(label: String, value: String, icon: ImageVector, iconColor: Color = MaterialTheme.colorScheme.primary, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MatchScoreBadge(score: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Recommend, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(4.dp))
            Text("$score% Match", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OnlineOfflineStatus(isOnline: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (isOnline) "Accepting Appointments Now" else "Currently Offline",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
        )
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
                "Travels up to ${String.format(Locale.US, "%.0f", rangeMiles)} mi ($${String.format(Locale.US, "%.0f", fee)} fee)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedVerifiedBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "badgeScale"
    )
    
    Surface(
        color = Color(0xFF1DA1F2).copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("ID VERIFIED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BeforeAfterCard(item: BeforeAfter) {
    var showNotes by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .size(300.dp, 400.dp),
        shape = RoundedCornerShape(24.dp),
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
                    shape = RoundedCornerShape(12.dp)
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
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

@Composable
fun TransformationCard(imageUrl: String) {
    Card(
        modifier = Modifier.size(280.dp, 380.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ServiceItem(service: Service, onClick: () -> Unit) {
    val containerColor = if (service.isBundle) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    val borderColor = if (service.isBundle) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
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
                    if (service.isBundle) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                if (service.description.isNotBlank() && service.isBundle) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(service.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${service.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format(Locale.US, "%.0f", service.price)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("BOOK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        review.userName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(review.userName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row {
                        repeat(5) { i ->
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (i < review.rating) Color(0xFFFFC107) else Color.LightGray
                            )
                        }
                    }
                }
            }
            Text(
                DateUtils.getRelativeTimeSpanString(review.timestampMillis ?: System.currentTimeMillis(), System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (review.comment.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}