package com.refreshme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
    onBack: () -> Unit,
    onChatStylist: (String) -> Unit,
    onTrackStylist: (String) -> Unit,
    onCancelBooking: (String) -> Unit,
    onBookAgain: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("Upcoming", "Past")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (val state = uiState) {
                is BookingsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is BookingsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is BookingsUiState.Success -> {
                    val filteredBookings = state.bookings.filter { booking ->
                        val isPast = booking.bookingStatus == BookingStatus.COMPLETED || 
                                     booking.bookingStatus == BookingStatus.CANCELLED ||
                                     booking.bookingStatus == BookingStatus.DECLINED
                        if (selectedTab == 0) !isPast else isPast
                    }

                    if (filteredBookings.isEmpty()) {
                        EmptyBookingsView()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredBookings) { booking ->
                                BookingItem(
                                    booking = booking,
                                    onChatClick = { onChatStylist(booking.stylistId) },
                                    onTrackClick = { onTrackStylist(booking.id) },
                                    onCancelClick = { onCancelBooking(booking.id) },
                                    onBookAgainClick = { onBookAgain(booking.stylistId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyBookingsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No bookings found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BookingItem(
    booking: Booking,
    onChatClick: () -> Unit,
    onTrackClick: () -> Unit,
    onCancelClick: () -> Unit,
    onBookAgainClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = booking.stylistPhotoUrl ?: "https://via.placeholder.com/150"
                    ),
                    contentDescription = "Stylist Image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.stylistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = booking.serviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$${booking.priceCents / 100}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Date & Time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val dateStr = booking.scheduledStart?.toDate()?.let {
                        SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(it)
                    } ?: "Pending..."
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val (statusText, statusColor) = when (booking.bookingStatus) {
                        BookingStatus.REQUESTED, BookingStatus.PENDING -> "Pending" to Color(0xFFFFA000)
                        BookingStatus.ACCEPTED, BookingStatus.DEPOSIT_PAID -> "Confirmed" to Color(0xFF4CAF50)
                        BookingStatus.ON_THE_WAY -> "On The Way" to Color(0xFF4CAF50)
                        BookingStatus.IN_PROGRESS -> "In Progress" to MaterialTheme.colorScheme.primary
                        BookingStatus.COMPLETED -> "Completed" to Color(0xFF4CAF50)
                        BookingStatus.CANCELLED, BookingStatus.DECLINED -> "Cancelled" to MaterialTheme.colorScheme.error
                        BookingStatus.REFUND_PROCESSING -> "Refunding" to Color(0xFFFFA000)
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (booking.bookingStatus) {
                    BookingStatus.REQUESTED, BookingStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onCancelClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = onChatClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Message")
                        }
                    }
                    BookingStatus.ACCEPTED, BookingStatus.DEPOSIT_PAID -> {
                        if (booking.isMobile) {
                            Button(
                                onClick = onTrackClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Track Stylist")
                            }
                        } else {
                            OutlinedButton(
                                onClick = onCancelClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = onChatClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Message")
                            }
                        }
                    }
                    BookingStatus.ON_THE_WAY, BookingStatus.IN_PROGRESS -> {
                        Button(
                            onClick = onTrackClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(if (booking.bookingStatus == BookingStatus.ON_THE_WAY) "Live Tracking" else "View Booking")
                        }
                    }
                    BookingStatus.COMPLETED -> {
                        OutlinedButton(
                            onClick = onBookAgainClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Book Again")
                        }
                        if (!booking.isRated) {
                            Button(
                                onClick = { /* TODO: Implement Rating */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rate Stylist")
                            }
                        }
                    }
                    BookingStatus.CANCELLED, BookingStatus.DECLINED, BookingStatus.REFUND_PROCESSING -> {
                        OutlinedButton(
                            onClick = onBookAgainClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Book Again")
                        }
                    }
                }
            }
        }
    }
}
