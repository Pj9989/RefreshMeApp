package com.refreshme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshme.ui.theme.RefreshMeTheme

data class Booking(
    val id: String,
    val stylistName: String,
    val service: String,
    val date: String,
    val time: String,
    val status: BookingStatus,
    val stylistImageRes: Int,
    val stylistStatus: String? = null
)

enum class BookingStatus {
    UPCOMING, COMPLETED, CANCELLED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen() {
    val bookings = remember {
        listOf(
            Booking("1", "Sarah Wilson", "Haircut + Beard Trim", "Dec 20, 2023", "2:00 PM", BookingStatus.UPCOMING, R.drawable.ic_launcher_foreground, stylistStatus = "On the way"),
            Booking("2", "Jessica Miller", "Manicure", "Dec 22, 2023", "10:00 AM", BookingStatus.UPCOMING, R.drawable.ic_launcher_foreground, stylistStatus = "Confirmed"),
            Booking("3", "Emily Davis", "Facial", "Dec 15, 2023", "11:30 AM", BookingStatus.COMPLETED, R.drawable.ic_launcher_foreground),
            Booking("4", "James Brown", "Haircut", "Dec 12, 2023", "4:00 PM", BookingStatus.CANCELLED, R.drawable.ic_launcher_foreground)
        )
    }

    var selectedTab by remember { mutableStateOf(BookingStatus.UPCOMING) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings") },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal
            ) {
                Tab(
                    selected = selectedTab == BookingStatus.UPCOMING,
                    onClick = { selectedTab = BookingStatus.UPCOMING },
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = selectedTab == BookingStatus.COMPLETED,
                    onClick = { selectedTab = BookingStatus.COMPLETED },
                    text = { Text("Completed") }
                )
                Tab(
                    selected = selectedTab == BookingStatus.CANCELLED,
                    onClick = { selectedTab = BookingStatus.CANCELLED },
                    text = { Text("Cancelled") }
                )
            }

            val filteredBookings = bookings.filter { it.status == selectedTab }

            if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedTab) {
                            BookingStatus.UPCOMING -> "No upcoming bookings yet"
                            BookingStatus.COMPLETED -> "Completed bookings will appear here"
                            BookingStatus.CANCELLED -> "No cancelled bookings"
                        }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredBookings) { booking ->
                        BookingCard(booking = booking)
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: Booking) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = booking.stylistImageRes),
                    contentDescription = booking.stylistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = booking.stylistName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = booking.service, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${booking.date} at ${booking.time}", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = booking.status.name,
                        color = when (booking.status) {
                            BookingStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                            BookingStatus.COMPLETED -> Color(0xFF388E3C) // Green
                            BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold
                    )
                    booking.stylistStatus?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when (booking.status) {
                    BookingStatus.UPCOMING -> {
                        OutlinedButton(onClick = { /* TODO: Handle cancel */ }) {
                            Text("Cancel")
                        }
                        Button(onClick = { /* TODO: Handle contact */ }) {
                            Text("Contact")
                        }
                        Button(onClick = { /* TODO: Handle reschedule */ }) {
                            Text("Reschedule")
                        }
                    }
                    BookingStatus.COMPLETED -> {
                        OutlinedButton(onClick = { /* TODO: Handle leave review */ }) {
                            Text("Leave a Review")
                        }
                        Button(onClick = { /* TODO: Handle book again */ }) {
                            Text("Book Again")
                        }
                    }
                    BookingStatus.CANCELLED -> {
                        Button(onClick = { /* TODO: Handle book again */ }) {
                            Text("Book Again")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookingsScreenPreview() {
    RefreshMeTheme {
        BookingsScreen()
    }
}
