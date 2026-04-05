package com.refreshme.booking

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.data.Service
import com.refreshme.data.Stylist
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    stylistId: String,
    serviceName: String?,
    servicePrice: Float,
    onBack: () -> Unit,
    onDateTimeClick: () -> Unit,
    onAsapClick: () -> Unit,
    onPaymentRequired: (clientSecret: String, depositAmount: Double) -> Unit,
    onBookingSuccess: () -> Unit,
    viewModel: NewBookingViewModel = viewModel()
) {
    val stylist by viewModel.stylist.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    val waitlistState by viewModel.waitlistState.collectAsState()
    
    val scrollState = rememberScrollState()

    var showWaitlistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
        if (serviceName != null && servicePrice > 0) {
            viewModel.selectService(Service(name = serviceName, price = servicePrice.toDouble()))
        }
    }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.RequiresPayment) {
            val state = bookingState as BookingState.RequiresPayment
            onPaymentRequired(state.clientSecret, state.depositAmount ?: ((selectedService?.price ?: 0.0) * 0.2))
        } else if (bookingState is BookingState.Success) {
            onBookingSuccess()
        }
    }
    
    LaunchedEffect(waitlistState) {
        if (waitlistState is WaitlistState.Success) {
            showWaitlistDialog = false
            // Show a success message (you could use a Snackbar here)
            viewModel.resetWaitlistState()
        }
    }

    if (showWaitlistDialog) {
        // DatePicker State for the Waitlist
        val datePickerState = rememberDatePickerState()
        
        DatePickerDialog(
            onDismissRequest = { showWaitlistDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.joinWaitlist(Date(millis))
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null && waitlistState !is WaitlistState.Loading
                ) {
                    if (waitlistState is WaitlistState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Join Waitlist")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaitlistDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text(text = "Select Date", modifier = Modifier.padding(16.dp)) },
                headline = { Text(text = "Join Waitlist For:", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // 1. Stylist Summary
                stylist?.let { s ->
                    StylistSummaryCard(s)
                    Spacer(Modifier.height(24.dp))
                } ?: Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                // 2. Service Selection
                SectionTitle("Service")
                if (selectedService != null && selectedService?.name?.isNotBlank() == true) {
                    ServiceSummaryCard(selectedService!!) {
                        viewModel.selectService(Service()) // Trigger reset
                    }
                } else {
                    val services = stylist?.services ?: emptyList()
                    if (services.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(services) { service ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.selectService(service) },
                                    label = { Text("${service.name} ($${String.format(Locale.US, "%.0f", service.price)})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        // If truly no services, show the auto-generated one if it exists
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                viewModel.selectService(Service(name = "General Consultation", price = 45.0, durationMinutes = 30))
                            },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Add General Consultation ($45.00)", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // 3. Date & Time Selection
                SectionTitle("Appointment Date & Time")
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DateTimeSelectionCard(
                        selectedDate = selectedDate,
                        onSelectClick = onDateTimeClick,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    AsapButton(onClick = onAsapClick)
                }
                
                Spacer(Modifier.height(16.dp))

                // Cancellation Roulette / Waitlist Option
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showWaitlistDialog = true },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No time slot works?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Join Waitlist to be notified of cancellations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // 4. Payment Breakdown
                SectionTitle("Payment Breakdown")
                PaymentBreakdownCard(price = selectedService?.price ?: 0.0)
                Spacer(Modifier.height(32.dp))

                // 5. Confirm Button
                val isOperable = (selectedService != null && selectedService?.name?.isNotBlank() == true) && selectedDate != null
                
                Button(
                    onClick = { viewModel.createBooking() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    enabled = isOperable && bookingState !is BookingState.Loading
                ) {
                    if (bookingState is BookingState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (isOperable) "Confirm & Pay Deposit" else "Please Complete Selection", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (bookingState is BookingState.Error) {
                    Text(
                        text = (bookingState as BookingState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (waitlistState is WaitlistState.Error) {
                    Text(
                        text = (waitlistState as WaitlistState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun StylistSummaryCard(stylist: Stylist) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stylist.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stylist.specialty ?: "Stylist", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ServiceSummaryCard(service: Service, onClear: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${service.durationMinutes ?: 30} mins", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            Text(
                "$${String.format(Locale.US, "%.2f", service.price)}",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DateTimeSelectionCard(selectedDate: Date?, onSelectClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onSelectClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp, 
            if (selectedDate == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (selectedDate == null) Icons.AutoMirrored.Filled.EventNote else Icons.Default.EventAvailable,
                contentDescription = null,
                tint = if (selectedDate == null) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (selectedDate == null) "Choose Date & Time" else {
                    val sdf = java.text.SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
                    sdf.format(selectedDate)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectedDate == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AsapButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(IntrinsicSize.Min) 
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ASAP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PaymentBreakdownCard(price: Double) {
    val deposit = price * 0.20
    val remaining = price - deposit
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            PriceRow("Service Price", price)
            Spacer(Modifier.height(8.dp))
            PriceRow("Deposit (20% paid now)", deposit, color = Color(0xFF4CAF50), isBold = true)
            Spacer(Modifier.height(8.dp))
            PriceRow("Remaining Balance", remaining)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Deposits are non-refundable unless the stylist cancels the appointment.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: Double, color: Color = MaterialTheme.colorScheme.onSurface, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isBold) color else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(
            "$${String.format(Locale.US, "%.2f", amount)}",
            color = color,
            fontSize = 16.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}