package com.refreshme.stylist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.RatingDialog
import com.refreshme.data.Booking
import com.refreshme.data.BookingRepository
import com.refreshme.data.BookingStatus
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class StylistBookingsFragment : Fragment() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var bookingRepository: BookingRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StylistBookingsScreen()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StylistBookingsScreen() {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Requests", "Upcoming", "Done", "Cancelled")
        
        var allBookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) }
        var showRatingDialogFor by remember { mutableStateOf<Booking?>(null) }

        val stylistId = auth.currentUser?.uid ?: ""

        LaunchedEffect(stylistId) {
            if (stylistId.isBlank()) {
                Log.e("StylistBookings", "No Stylist ID found (User not logged in?)")
                return@LaunchedEffect
            }
            
            isLoading = true
            bookingRepository.getStylistBookings(stylistId).collect { bookings ->
                Log.d("StylistBookings", "Received ${bookings.size} bookings for stylist: $stylistId")
                allBookings = bookings
                isLoading = false
                isRefreshing = false
            }
        }

        val filteredBookings = remember(selectedTab, allBookings) {
            val statusFilter = when (selectedTab) {
                0 -> listOf(BookingStatus.REQUESTED, BookingStatus.PENDING)
                1 -> listOf(BookingStatus.DEPOSIT_PAID, BookingStatus.ACCEPTED, BookingStatus.ON_THE_WAY, BookingStatus.IN_PROGRESS)
                2 -> listOf(BookingStatus.COMPLETED)
                else -> listOf(BookingStatus.CANCELLED, BookingStatus.DECLINED)
            }
            
            allBookings.filter { it.bookingStatus in statusFilter }
                .sortedByDescending { it.requestedAt?.seconds ?: 0 }
        }

        if (showRatingDialogFor != null) {
            RatingDialog(
                booking = showRatingDialogFor!!,
                onDismiss = { showRatingDialogFor = null },
                onSubmit = { rating, comment ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        bookingRepository.submitCustomerReview(showRatingDialogFor!!, rating, comment)
                        showRatingDialogFor = null
                        Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show()
                    }
                },
                isStylistRatingCustomer = true
            )
        }

        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = { Text("Client Management", fontWeight = FontWeight.Black) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent, 
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        actions = {
                            IconButton(onClick = { 
                                isRefreshing = true
                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(800)
                                    isRefreshing = false
                                }
                            }) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Refresh", 
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                                        
                                        if (index == 0) {
                                            val requestCount = allBookings.count { it.bookingStatus == BookingStatus.REQUESTED || it.bookingStatus == BookingStatus.PENDING }
                                            if (requestCount > 0) {
                                                Spacer(Modifier.width(4.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(requestCount.toString(), color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading || isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredBookings.isEmpty()) {
                    EmptyBookingsView(tabs[selectedTab])
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredBookings, key = { it.id }) { booking ->
                            BookingActionCard(
                                booking = booking,
                                onAccept = { handleAcceptBooking(booking) },
                                onDecline = { handleDeclineBooking(booking) },
                                onStart = { handleStartBooking(booking) },
                                onTrack = { handleTrackBooking(booking) },
                                onCancel = { handleCancelBooking(booking) },
                                onMessage = { handleMessageCustomer(booking) },
                                onRate = { showRatingDialogFor = booking }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BookingActionCard(
        booking: Booking,
        onAccept: () -> Unit,
        onDecline: () -> Unit,
        onStart: () -> Unit,
        onTrack: () -> Unit,
        onCancel: () -> Unit,
        onMessage: () -> Unit,
        onRate: () -> Unit
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp),
            border = if (booking.bookingStatus == BookingStatus.REQUESTED || booking.bookingStatus == BookingStatus.PENDING || booking.bookingStatus == BookingStatus.DEPOSIT_PAID) 
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) 
                else null
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = if (booking.customerName.isNotEmpty()) booking.customerName.take(1).uppercase() else "?"
                        Text(initial, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(booking.customerName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
                        Text(booking.serviceName, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                    val priceDisplay = (booking.priceCents.toDouble() / 100.0)
                    Text("$${String.format(Locale.US, "%.0f", priceDisplay)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
                }

                // Tags for Modern Salon Toggles
                if (booking.isSilentAppointment || booking.isSensoryFriendly) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (booking.isSilentAppointment) {
                            Surface(
                                color = Color(0xFF7E57C2), // Deep Purple
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Silent",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (booking.isSensoryFriendly) {
                            Surface(
                                color = Color(0xFF26A69A), // Teal
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Sensory Friendly",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                if (booking.isEvent) {
                    Spacer(Modifier.height(8.dp))
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

                if (booking.isMobile) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("House Call", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    val timeStr = booking.requestedAt?.let { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(it.toDate()) } ?: "Time Pending"
                    Text(text = timeStr, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                }

                if (booking.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(booking.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (booking.bookingStatus) {
                        BookingStatus.REQUESTED, BookingStatus.PENDING -> {
                            Button(onClick = onAccept, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                                Text("Accept")
                            }
                            OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Decline")
                            }
                        }
                        BookingStatus.DEPOSIT_PAID, BookingStatus.ACCEPTED, BookingStatus.ON_THE_WAY, BookingStatus.IN_PROGRESS -> {
                            if (booking.isMobile) {
                                Button(
                                    onClick = onTrack, 
                                    modifier = Modifier.weight(1.5f), 
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Open Mobile Map")
                                }
                            } else {
                                val buttonText = if (booking.bookingStatus == BookingStatus.IN_PROGRESS) "Complete Session" else "Start Session"
                                val buttonColor = if (booking.bookingStatus == BookingStatus.IN_PROGRESS) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                
                                Button(
                                    onClick = onStart, 
                                    modifier = Modifier.weight(1.5f), 
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                                ) {
                                    Text(buttonText)
                                }
                            }
                            IconButton(onClick = onMessage, modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Message", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        BookingStatus.COMPLETED -> {
                            if (!booking.isCustomerRated) {
                                Button(onClick = onRate, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Text("Rate Client")
                                }
                            } else {
                                OutlinedButton(onClick = onMessage, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.MailOutline, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Message Client")
                                }
                            }
                        }
                        else -> {
                            OutlinedButton(onClick = onMessage, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.MailOutline, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Message Client")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EmptyBookingsView(tabName: String) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("No $tabName", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Your $tabName will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }

    private fun handleAcceptBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                bookingRepository.acceptBookingRequest(booking.id)
                Toast.makeText(context, "Booking Accepted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("StylistBookings", "Failed to accept", e)
            }
        }
    }

    private fun handleDeclineBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                bookingRepository.declineBookingRequest(booking.id)
                Toast.makeText(context, "Booking Declined", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("StylistBookings", "Failed to decline", e)
            }
        }
    }

    private fun handleStartBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val nextStatus = if (booking.bookingStatus == BookingStatus.IN_PROGRESS) BookingStatus.COMPLETED else BookingStatus.IN_PROGRESS
                bookingRepository.updateBookingStatus(booking.id, nextStatus)
                val msg = if (nextStatus == BookingStatus.COMPLETED) "Session Completed!" else "Session Started!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("StylistBookings", "Failed to update status", e)
            }
        }
    }
    
    private fun handleTrackBooking(booking: Booking) {
        try {
            findNavController().navigate(
                R.id.activeMobileBookingFragment,
                Bundle().apply { 
                    putString("bookingId", booking.id)
                    putBoolean("isStylist", true)
                }
            )
        } catch (e: Exception) {
            Log.e("StylistBookings", "Navigation to Tracking failed", e)
        }
    }

    private fun handleCancelBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                bookingRepository.updateBookingStatus(booking.id, BookingStatus.CANCELLED)
                Toast.makeText(context, "Booking Cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("StylistBookings", "Failed to cancel", e)
            }
        }
    }

    private fun handleMessageCustomer(booking: Booking) {
        try {
            findNavController().navigate(
                R.id.chatFragment,
                Bundle().apply { putString("otherUserId", booking.customerId) }
            )
        } catch (e: Exception) {
            Log.e("StylistBookings", "Navigation failed", e)
        }
    }
}