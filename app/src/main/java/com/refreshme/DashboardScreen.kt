package com.refreshme

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import com.refreshme.data.Stylist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CustomerDashboardViewModel = viewModel(),
    onFindStylist: () -> Unit,
    onMyBookings: () -> Unit,
    onStylistClick: (Stylist) -> Unit,
    onVirtualTryOn: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val userName = uiState.userName
    val activeBooking = uiState.upcomingBooking
    val flashDeals = uiState.flashDeals
    val favorites = uiState.savedStylists
    val recommended = uiState.nearbyStylists

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RefreshMe", fontWeight = FontWeight.Black, fontSize = 22.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onFindStylist) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Welcome back, $userName \uD83D\uDC4B",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        "Book Cut", Icons.Default.ContentCut, MaterialTheme.colorScheme.primary, Modifier.weight(1f),
                        onClick = onFindStylist
                    )
                    QuickActionCard(
                        "House Call", Icons.Default.DirectionsCar, Color(0xFF64B5F6), Modifier.weight(1f),
                        onClick = { onFindStylist() } // Can pass a filter bundle in the future
                    )
                    QuickActionCard(
                        "AI Try-On", Icons.Default.Face, Color(0xFFE91E63), Modifier.weight(1f),
                        onClick = onVirtualTryOn
                    )
                }
            }

            // Active/Upcoming Appointment Card
            if (activeBooking != null) {
                item {
                    Text(
                        "Upcoming Appointment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    ActiveAppointmentCard(
                        booking = activeBooking,
                        onClick = { onMyBookings() }
                    )
                }
            }

            // Flash Deals (Horizontal Scroll)
            if (flashDeals.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Live Flash Deals \u26A1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(flashDeals) { deal ->
                            FlashDealCard(deal, onClick = { onStylistClick(deal) })
                        }
                    }
                }
            }

            // Favorites
            if (favorites.isNotEmpty()) {
                item {
                    Text("Your Squad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(favorites) { fav ->
                            FavoriteStylistCircle(fav, onClick = { onStylistClick(fav) })
                        }
                    }
                }
            }

            // Recommended
            if (recommended.isNotEmpty()) {
                item {
                    Text("Recommended Near You", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        recommended.take(3).forEach { stylist ->
                            NearbyStylistItem(stylist, onClick = { onStylistClick(stylist) })
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun ActiveAppointmentCard(booking: Booking, onClick: () -> Unit) {
    val isMobile = booking.isMobile
    val bgColors = if (isMobile) {
        listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    }
    
    val timestamp = booking.scheduledStart?.seconds?.times(1000) ?: booking.requestedAt?.seconds?.times(1000) ?: System.currentTimeMillis()
    val dateStr = DateUtils.getRelativeTimeSpanString(timestamp).toString()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.background(Brush.horizontalGradient(bgColors)).padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape).padding(8.dp)) {
                    Icon(if (isMobile) Icons.Default.DirectionsCar else Icons.Default.Event, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Next Appointment", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(dateStr, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = booking.stylistPhotoUrl,
                        contentDescription = null,
                        placeholder = fallbackPainter,
                        error = fallbackPainter,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(booking.stylistName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(booking.serviceName, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                
                if (booking.bookingStatus == BookingStatus.ON_THE_WAY) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                            Spacer(Modifier.width(6.dp))
                            Text("En Route", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else if (booking.bookingStatus == BookingStatus.ACCEPTED) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirmed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FlashDealCard(stylist: Stylist, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(260.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                AsyncImage(
                    model = stylist.displayImageUrl,
                    contentDescription = null,
                    placeholder = fallbackPainter,
                    error = fallbackPainter,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${stylist.currentFlashDeal?.discountPercentage ?: 0}% OFF", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stylist.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stylist.currentFlashDeal?.title ?: "Special Offer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FavoriteStylistCircle(stylist: Stylist, onClick: () -> Unit) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = stylist.displayImageUrl,
            contentDescription = null,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stylist.name.split(" ").first(), 
            maxLines = 1, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NearbyStylistItem(stylist: Stylist, onClick: () -> Unit) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = stylist.displayImageUrl,
                contentDescription = null,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stylist.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stylist.specialty ?: "Stylist", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tertiary CTA: "Virtual Hair Try-On" — AI-powered feature
            Button(
                onClick = { /* onVirtualTryOn */ },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "\u2728  Virtual Hair Try-On",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}