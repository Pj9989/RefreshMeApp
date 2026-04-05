package com.refreshme

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
    onBack: () -> Unit,
    onChatStylist: (String) -> Unit = {},
    onTrackStylist: (String) -> Unit = {},
    onCancelBooking: (String) -> Unit = {},
    onBookAgain: (String) -> Unit = {},
    onViewReceipt: (Booking) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val showRatingDialogFor by viewModel.showRatingDialog.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var bookingToReschedule by remember { mutableStateOf<Booking?>(null) }
    val context = LocalContext.current

    val tabs = listOf("Upcoming", "Past")

    if (bookingToReschedule != null) {
        RescheduleDialog(
            booking = bookingToReschedule!!,
            onDismiss = { bookingToReschedule = null },
            onConfirm = { newDate ->
                viewModel.rescheduleBooking(bookingToReschedule!!.id, newDate)
                bookingToReschedule = null
            }
        )
    }

    if (showRatingDialogFor != null) {
        RatingDialog(
            booking = showRatingDialogFor!!,
            onDismiss = { viewModel.closeRatingDialog() },
            onSubmit = { rating, comment ->
                viewModel.submitReview(showRatingDialogFor!!, rating, comment)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bookings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
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
                        EmptyBookingsView(selectedTab == 0)
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
                                    onRescheduleClick = { bookingToReschedule = booking },
                                    onBookAgainClick = { onBookAgain(booking.stylistId) },
                                    onRateClick = { viewModel.openRatingDialog(booking) },
                                    onAddToCalendar = { addToCalendar(context, booking) },
                                    onViewReceipt = { onViewReceipt(booking) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun addToCalendar(context: android.content.Context, booking: Booking) {
    val startTime = booking.scheduledStart?.toDate()?.time ?: return
    val endTime = startTime + (60 * 60 * 1000)
    
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Appointment with ${booking.stylistName} (RefreshMe)")
        putExtra(CalendarContract.Events.DESCRIPTION, "Service: ${booking.serviceName}\nNotes: ${booking.notes}")
        putExtra(CalendarContract.Events.EVENT_LOCATION, booking.customerAddress ?: "In-Salon")
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
    }
    context.startActivity(intent)
}

@Composable
fun EmptyBookingsView(isUpcoming: Boolean) {
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
            text = if (isUpcoming) "No upcoming bookings" else "No past bookings found",
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
    onRescheduleClick: () -> Unit,
    onBookAgainClick: () -> Unit,
    onRateClick: () -> Unit,
    onAddToCalendar: () -> Unit,
    onViewReceipt: () -> Unit
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

            if (booking.isEvent) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Groups, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${booking.eventType ?: "Group Event"} • Size: ${booking.groupSize}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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

            // Unified Actions based on status
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (booking.bookingStatus) {
                    BookingStatus.REQUESTED, BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.DEPOSIT_PAID -> {
                        val isConfirmed = booking.bookingStatus == BookingStatus.ACCEPTED || booking.bookingStatus == BookingStatus.DEPOSIT_PAID
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isConfirmed) {
                                if (booking.isMobile) {
                                    Button(onClick = onTrackClick, modifier = Modifier.weight(1f)) {
                                        Text("Track", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                OutlinedButton(onClick = onAddToCalendar, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Icon(Icons.Default.Event, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Calendar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(onClick = onRescheduleClick, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text("Reschedule", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                                Button(onClick = onChatClick, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text("Message", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                            }
                        }
                        
                        if (isConfirmed) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onRescheduleClick, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text("Reschedule", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                                Button(onClick = onChatClick, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text("Message", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                }
                            }
                        }
                        
                        OutlinedButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel Booking", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    BookingStatus.ON_THE_WAY, BookingStatus.IN_PROGRESS -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onTrackClick, modifier = Modifier.weight(1f)) {
                                Text(if (booking.bookingStatus == BookingStatus.ON_THE_WAY) "Live Tracking" else "View Booking")
                            }
                            Button(onClick = onChatClick, modifier = Modifier.weight(1f)) {
                                Text("Message")
                            }
                        }
                    }
                    BookingStatus.COMPLETED -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onViewReceipt, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Receipt, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Receipt")
                            }
                            if (!booking.isRated) {
                                Button(onClick = onRateClick, modifier = Modifier.weight(1f)) {
                                    Text("Rate")
                                }
                            } else {
                                OutlinedButton(onClick = onBookAgainClick, modifier = Modifier.weight(1f)) {
                                    Text("Book Again")
                                }
                            }
                        }
                    }
                    BookingStatus.CANCELLED, BookingStatus.DECLINED, BookingStatus.REFUND_PROCESSING -> {
                        Button(onClick = onBookAgainClick, modifier = Modifier.fillMaxWidth()) {
                            Text("Book Again")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RescheduleDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTimeStr by remember { mutableStateOf("") }
    var finalDate by remember { mutableStateOf<Date?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Reschedule Appointment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Select a new date and time for your appointment with ${booking.stylistName}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                WeeklyCalendarView(
                    selectedDate = selectedDate,
                    onDateSelected = { 
                        selectedTimeStr = ""
                        finalDate = null
                        selectedDate = it 
                    }
                )

                if (selectedDate != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    val times = listOf("9:00 AM", "10:30 AM", "1:00 PM", "2:30 PM", "4:00 PM")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        times.forEach { time ->
                            FilterChip(
                                selected = selectedTimeStr == time,
                                onClick = { 
                                    selectedTimeStr = time
                                    val calendar = Calendar.getInstance()
                                    calendar.time = selectedDate!!
                                    val isPm = time.contains("PM", ignoreCase = true)
                                    val timeParts = time.replace(" AM", "", true).replace(" PM", "", true).split(":")
                                    var hour = timeParts[0].toInt()
                                    if (isPm && hour < 12) hour += 12
                                    if (!isPm && hour == 12) hour = 0
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                                    calendar.set(Calendar.SECOND, 0)
                                    finalDate = calendar.time
                                },
                                label = { Text(time) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { finalDate?.let { onConfirm(it) } },
                        enabled = finalDate != null
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}